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
  config_check = {
    quorums_file = "./controls/backend_check_quorums";
  }; -- config_check
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
  cpu_limiter = {
    active_check_subnet_default = true;
    disable_file = "./controls/cpu_limiter_disabled";
    active_check_subnet_file = "./controls/active_check_subnets_list";
  }; -- cpu_limiter
  addrs = {
    {
      ip = "*";
      port = 80;
      disabled = get_int_var("disable_external", 0);
    };
    {
      ip = "*";
      port = 443;
      disabled = get_int_var("disable_external", 0);
    };
  }; -- addrs
  unistat = {
    addrs = {
      {
        ip = "*";
        port = get_port_var("port", 2);
        disabled = get_int_var("disable_external", 0);
      };
    }; -- addrs
    hide_legacy_signals = true;
  }; -- unistat
  sd = {
    client_name = "awacs-l7-balancer(bs.yandex.ru:bs.yandex.ru_sas)";
    host = "sd.yandex.net";
    port = 8080;
    connect_timeout = "50ms";
    request_timeout = "1s";
    cache_dir = get_str_var("sd_cache_dir", "./sd_cache");
  }; -- sd
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
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", 80, "/place/db/www/logs");
        http = {
          maxlen = 65536;
          maxreq = 65536;
          keepalive = true;
          no_keepalive_file = "./controls/keepalive_disabled";
          keepalive_drop_probability = 0.010;
          events = {
            stats = "report";
          }; -- events
          accesslog = {
            log = get_log_path("access_log", 80, "/place/db/www/logs");
            report = {
              uuid = "service_total";
              ranges = get_str_var("default_ranges");
              input_size_ranges = "32,64,128,256,512,1024,4096,8192,16384,131072,524288,1048576,2097152";
              output_size_ranges = "512,1024,4096,8192,16384,32768,65536,131072,262144,524288,1048576,2097152,4194304,8388608";
              just_storage = false;
              disable_robotness = true;
              disable_sslness = true;
              events = {
                stats = "report";
              }; -- events
              cookie_policy = {
                uuid = "service_total";
                default_yandex_policies = "stable";
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
                  default = {
                    priority = 1;
                    headers = {
                      create = {
                        ["X-Yabs-Balancer-Samogon-Key"] = "101";
                      }; -- create
                      regexp = {
                        default = {
                          priority = 1;
                          regexp_host = {
                            ["an.webvisor.org"] = {
                              priority = 5;
                              pattern = "((an\\.webvisor\\.com)|(an\\.webvisor\\.org))(:80)?";
                              case_insensitive = true;
                              shared = {
                                uuid = "1847316178606023523";
                                regexp = {
                                  legacy_metrika_redirect = {
                                    priority = 4;
                                    match_or = {
                                      {
                                        match_fsm = {
                                          url = "/+watch/.*";
                                          case_insensitive = false;
                                          surround = false;
                                        }; -- match_fsm
                                      };
                                      {
                                        match_fsm = {
                                          url = "/+informer/.*";
                                          case_insensitive = false;
                                          surround = false;
                                        }; -- match_fsm
                                      };
                                    }; -- match_or
                                    shared = {
                                      uuid = "243030124386208781";
                                      report = {
                                        uuid = "metrika_report";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        headers = {
                                          create_func = {
                                            ["X-Forwarded-Proto-Version"] = "proto";
                                            ["X-Real-Port"] = "realport";
                                            ["X-Request-Origin-IP"] = "realip";
                                            ["X-Request-Origin-Port"] = "realport";
                                            ["X-Yabs-Balancer-Ja3"] = "ja3";
                                          }; -- create_func
                                          create_func_weak = {
                                            ["X-Forwarded-Proto"] = "scheme";
                                            ["X-Real-IP"] = "realip";
                                          }; -- create_func_weak
                                          response_headers = {
                                            create = {
                                              ["Strict-Transport-Security"] = "max-age=31536000";
                                              ["Timing-Allow-Origin"] = "*";
                                              ["X-XSS-Protection"] = "1; mode=block";
                                            }; -- create
                                            headers = {
                                              create = {
                                                Location = "replace_me";
                                              }; -- create
                                              rewrite = {
                                                actions = {
                                                  {
                                                    regexp = ".*";
                                                    global = false;
                                                    literal = false;
                                                    case_insensitive = false;
                                                    header_name = "Location";
                                                    rewrite = "https://mc.yandex.ru%{url}";
                                                  };
                                                }; -- actions
                                                errordocument = {
                                                  status = 302;
                                                  force_conn_close = false;
                                                  remain_headers = "Location";
                                                }; -- errordocument
                                              }; -- rewrite
                                            }; -- headers
                                          }; -- response_headers
                                        }; -- headers
                                      }; -- report
                                    }; -- shared
                                  }; -- legacy_metrika_redirect
                                  jstracer = {
                                    priority = 3;
                                    match_fsm = {
                                      url = "/+jstracer";
                                      case_insensitive = false;
                                      surround = false;
                                    }; -- match_fsm
                                    shared = {
                                      uuid = "4544929115444722299";
                                      report = {
                                        uuid = "jstracer";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        headers = {
                                          create = {
                                            Host = "jstracer.yandex.net";
                                          }; -- create
                                          headers = {
                                            create_func = {
                                              ["X-Forwarded-Proto-Version"] = "proto";
                                              ["X-Real-Port"] = "realport";
                                              ["X-Request-Origin-IP"] = "realip";
                                              ["X-Request-Origin-Port"] = "realport";
                                              ["X-Yabs-Balancer-Ja3"] = "ja3";
                                            }; -- create_func
                                            create_func_weak = {
                                              ["X-Forwarded-Proto"] = "scheme";
                                              ["X-Real-IP"] = "realip";
                                            }; -- create_func_weak
                                            response_headers = {
                                              create = {
                                                ["Strict-Transport-Security"] = "max-age=31536000";
                                                ["Timing-Allow-Origin"] = "*";
                                                ["X-XSS-Protection"] = "1; mode=block";
                                              }; -- create
                                              request_replier = {
                                                sink = {
                                                  report = {
                                                    uuid = "jstracer_mirrors";
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
                                                      connection_attempts = 3;
                                                      sd = {
                                                        endpoint_sets = {
                                                          {
                                                            cluster_name = "sas";
                                                            endpoint_set_id = "jstracer10";
                                                          };
                                                          {
                                                            cluster_name = "man";
                                                            endpoint_set_id = "jstracer10";
                                                          };
                                                          {
                                                            cluster_name = "vla";
                                                            endpoint_set_id = "jstracer10";
                                                          };
                                                        }; -- endpoint_sets
                                                        proxy_options = {
                                                          resolve_timeout = "10ms";
                                                          connect_timeout = "100ms";
                                                          backend_timeout = "500ms";
                                                          fail_on_5xx = true;
                                                          http_backend = true;
                                                          buffering = false;
                                                          keepalive_count = 0;
                                                          need_resolve = false;
                                                        }; -- proxy_options
                                                        active = {
                                                          delay = "10s";
                                                          request = "GET /ping HTTP/1.1\nHost: jstracer.yandex.net\n\n";
                                                          steady = true;
                                                        }; -- active
                                                      }; -- sd
                                                      attempts_rate_limiter = {
                                                        limit = 0.200;
                                                        coeff = 0.990;
                                                        switch_default = true;
                                                      }; -- attempts_rate_limiter
                                                      on_error = {
                                                        errordocument = {
                                                          status = 503;
                                                          force_conn_close = false;
                                                          content = "Service Unavailable";
                                                        }; -- errordocument
                                                      }; -- on_error
                                                    }; -- balancer2
                                                  }; -- report
                                                }; -- sink
                                                enable_failed_requests_replication = false;
                                                rate = 0.110;
                                                rate_file = "./controls/request_repl_jstracer_mirror.ratefile";
                                                request_replier = {
                                                  sink = {
                                                    report = {
                                                      uuid = "jstracer_engine";
                                                      ranges = get_str_var("default_ranges");
                                                      just_storage = false;
                                                      disable_robotness = true;
                                                      disable_sslness = true;
                                                      events = {
                                                        stats = "report";
                                                      }; -- events
                                                      shared = {
                                                        uuid = "common";
                                                      }; -- shared
                                                    }; -- report
                                                  }; -- sink
                                                  enable_failed_requests_replication = false;
                                                  rate = 0.000;
                                                  rate_file = "./controls/request_repl_jstracer_engine.ratefile";
                                                  balancer2 = {
                                                    simple_policy = {};
                                                    attempts = 1;
                                                    rr = {
                                                      weights_file = "./controls/traffic_control.weights";
                                                      jstracer_main = {
                                                        weight = 1.000;
                                                        balancer2 = {
                                                          unique_policy = {};
                                                          attempts = 2;
                                                          connection_attempts = 3;
                                                          sd = {
                                                            endpoint_sets = {
                                                              {
                                                                cluster_name = "sas";
                                                                endpoint_set_id = "jstracer0";
                                                              };
                                                              {
                                                                cluster_name = "man";
                                                                endpoint_set_id = "jstracer0";
                                                              };
                                                              {
                                                                cluster_name = "vla";
                                                                endpoint_set_id = "jstracer0";
                                                              };
                                                              {
                                                                cluster_name = "sas";
                                                                endpoint_set_id = "jstracer1";
                                                              };
                                                              {
                                                                cluster_name = "man";
                                                                endpoint_set_id = "jstracer1";
                                                              };
                                                              {
                                                                cluster_name = "vla";
                                                                endpoint_set_id = "jstracer1";
                                                              };
                                                              {
                                                                cluster_name = "sas";
                                                                endpoint_set_id = "jstracer2";
                                                              };
                                                              {
                                                                cluster_name = "man";
                                                                endpoint_set_id = "jstracer2";
                                                              };
                                                              {
                                                                cluster_name = "vla";
                                                                endpoint_set_id = "jstracer2";
                                                              };
                                                            }; -- endpoint_sets
                                                            proxy_options = {
                                                              resolve_timeout = "10ms";
                                                              connect_timeout = "100ms";
                                                              backend_timeout = "500ms";
                                                              fail_on_5xx = true;
                                                              http_backend = true;
                                                              buffering = false;
                                                              keepalive_count = 0;
                                                              need_resolve = false;
                                                            }; -- proxy_options
                                                            active = {
                                                              delay = "10s";
                                                              request = "GET /ping HTTP/1.1\nHost: jstracer.yandex.net\n\n";
                                                              steady = true;
                                                            }; -- active
                                                          }; -- sd
                                                          attempts_rate_limiter = {
                                                            limit = 0.200;
                                                            coeff = 0.990;
                                                            switch_default = true;
                                                          }; -- attempts_rate_limiter
                                                          on_error = {
                                                            errordocument = {
                                                              status = 503;
                                                              force_conn_close = false;
                                                              content = "Service Unavailable";
                                                            }; -- errordocument
                                                          }; -- on_error
                                                        }; -- balancer2
                                                      }; -- jstracer_main
                                                      jstracer_cut = {
                                                        weight = -1.000;
                                                        response_headers = {
                                                          create = {
                                                            ["Access-Control-Allow-Headers"] = "User-Agent, Content-Type";
                                                            ["Access-Control-Allow-Methods"] = "POST, OPTIONS";
                                                            ["Access-Control-Allow-Origin"] = "*";
                                                            ["Access-Control-Max-Age"] = "86400";
                                                            Allow = "POST, OPTIONS";
                                                          }; -- create
                                                          errordocument = {
                                                            status = 204;
                                                            force_conn_close = false;
                                                          }; -- errordocument
                                                        }; -- response_headers
                                                      }; -- jstracer_cut
                                                    }; -- rr
                                                  }; -- balancer2
                                                }; -- request_replier
                                              }; -- request_replier
                                            }; -- response_headers
                                          }; -- headers
                                        }; -- headers
                                      }; -- report
                                    }; -- shared
                                  }; -- jstracer
                                  base_info_restrict = {
                                    priority = 2;
                                    match_or = {
                                      {
                                        match_fsm = {
                                          path = "/+base_info(/.*)?";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                      };
                                      {
                                        match_fsm = {
                                          path = "/+meta_info(/.*)?";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                      };
                                      {
                                        match_fsm = {
                                          path = "/+status(/.*)?";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                      };
                                    }; -- match_or
                                    shared = {
                                      uuid = "3149712711745469625";
                                      report = {
                                        uuid = "base_info_restrict";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        errordocument = {
                                          status = 403;
                                          content = "Forbidden";
                                          force_conn_close = false;
                                        }; -- errordocument
                                      }; -- report
                                    }; -- shared
                                  }; -- base_info_restrict
                                  default = {
                                    priority = 1;
                                    shared = {
                                      uuid = "3357443050276055493";
                                      report = {
                                        uuid = "default";
                                        ranges = get_str_var("default_ranges");
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        events = {
                                          stats = "report";
                                        }; -- events
                                        shared = {
                                          uuid = "common";
                                          headers = {
                                            create_func = {
                                              ["X-Forwarded-Proto-Version"] = "proto";
                                              ["X-Real-Port"] = "realport";
                                              ["X-Request-Origin-IP"] = "realip";
                                              ["X-Request-Origin-Port"] = "realport";
                                              ["X-Yabs-Balancer-Ja3"] = "ja3";
                                            }; -- create_func
                                            create_func_weak = {
                                              ["X-Forwarded-Proto"] = "scheme";
                                              ["X-Real-IP"] = "realip";
                                            }; -- create_func_weak
                                            response_headers = {
                                              create = {
                                                ["Strict-Transport-Security"] = "max-age=31536000";
                                                ["Timing-Allow-Origin"] = "*";
                                                ["X-XSS-Protection"] = "1; mode=block";
                                              }; -- create
                                              compressor = {
                                                enable_compression = true;
                                                enable_decompression = false;
                                                compression_codecs = "gzip";
                                                request_replier = {
                                                  sink = {
                                                    report = {
                                                      uuid = "bs_mirrors";
                                                      ranges = get_str_var("default_ranges");
                                                      just_storage = false;
                                                      disable_robotness = true;
                                                      disable_sslness = true;
                                                      events = {
                                                        stats = "report";
                                                      }; -- events
                                                      cookie_hasher = {
                                                        cookie = "yandexuid";
                                                        file_switch = "./controls/disable_cookie_hasher";
                                                        balancer2 = {
                                                          watermark_policy = {
                                                            lo = 0.100;
                                                            hi = 0.150;
                                                            params_file = "./controls/watermark_policy.params_file";
                                                            unique_policy = {};
                                                          }; -- watermark_policy
                                                          attempts = 3;
                                                          rendezvous_hashing = {
                                                            weights_file = "./controls/traffic_control.weights";
                                                            mirror_sas1 = {
                                                              weight = 1.000;
                                                              report = {
                                                                uuid = "mirror_sas1";
                                                                ranges = get_str_var("default_ranges");
                                                                backend_time_ranges = "100ms,150ms,200ms,300ms,500ms";
                                                                just_storage = false;
                                                                disable_robotness = true;
                                                                disable_sslness = true;
                                                                events = {
                                                                  stats = "report";
                                                                }; -- events
                                                                balancer2 = {
                                                                  active_policy = {
                                                                    unique_policy = {};
                                                                  }; -- active_policy
                                                                  attempts = 1;
                                                                  rendezvous_hashing = {
                                                                    unpack(gen_proxy_backends({
                                                                      { "sas2-3217-sas-yabs-frontend-serve-f33-15529.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c08:ec96:10d:f309:0:3ca9"; };
                                                                      { "sas2-3220-sas-yabs-frontend-serve-f33-15529.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c08:ec95:10d:f309:0:3ca9"; };
                                                                      { "sas2-3224-sas-yabs-frontend-serve-f33-15529.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c08:ec91:10d:f309:0:3ca9"; };
                                                                      { "sas2-3225-sas-yabs-frontend-serve-f33-15529.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c08:eca9:10d:f309:0:3ca9"; };
                                                                      { "sas2-3227-sas-yabs-frontend-serve-f33-15529.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c08:ec8d:10d:f309:0:3ca9"; };
                                                                      { "sas2-3231-sas-yabs-frontend-serve-f33-15529.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c08:eca5:10d:f309:0:3ca9"; };
                                                                      { "sas2-3236-sas-yabs-frontend-serve-f33-15529.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c08:eca7:10d:f309:0:3ca9"; };
                                                                      { "sas2-3237-sas-yabs-frontend-serve-f33-15529.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c08:ec8c:10d:f309:0:3ca9"; };
                                                                      { "sas2-3242-sas-yabs-frontend-serve-f33-15529.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c08:ec90:10d:f309:0:3ca9"; };
                                                                      { "sas2-3250-sas-yabs-frontend-serve-f33-15529.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c08:ec99:10d:f309:0:3ca9"; };
                                                                      { "sas2-3271-sas-yabs-frontend-serve-f33-15529.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c08:ec97:10d:f309:0:3ca9"; };
                                                                      { "sas2-3273-sas-yabs-frontend-serve-f33-15529.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c08:ec98:10d:f309:0:3ca9"; };
                                                                    }, {
                                                                      resolve_timeout = "10ms";
                                                                      connect_timeout = "50ms";
                                                                      backend_timeout = "10s";
                                                                      fail_on_5xx = true;
                                                                      http_backend = true;
                                                                      buffering = false;
                                                                      keepalive_count = 1;
                                                                      need_resolve = true;
                                                                      keepalive_timeout = "60s";
                                                                    }))
                                                                  }; -- rendezvous_hashing
                                                                }; -- balancer2
                                                              }; -- report
                                                            }; -- mirror_sas1
                                                          }; -- rendezvous_hashing
                                                        }; -- balancer2
                                                      }; -- cookie_hasher
                                                    }; -- report
                                                  }; -- sink
                                                  enable_failed_requests_replication = true;
                                                  rate = 0.010;
                                                  rate_file = "./controls/request_repl.ratefile";
                                                  hdrcgi = {
                                                    hdr_from_cgi = {
                                                      ["X-Yandex-Retry"] = "X-Yandex-Retry";
                                                    }; -- hdr_from_cgi
                                                    regexp = {
                                                      client_retry = {
                                                        priority = 4;
                                                        match_fsm = {
                                                          header = {
                                                            name = "X-Yandex-Retry";
                                                            value = ".*";
                                                          }; -- header
                                                          case_insensitive = true;
                                                          surround = false;
                                                        }; -- match_fsm
                                                        report = {
                                                          uuid = "client_retries";
                                                          ranges = get_str_var("default_ranges");
                                                          just_storage = false;
                                                          disable_robotness = true;
                                                          disable_sslness = true;
                                                          events = {
                                                            stats = "report";
                                                          }; -- events
                                                          hasher = {
                                                            mode = "random";
                                                            shared = {
                                                              uuid = "yabs_frontend_backends";
                                                              balancer2 = {
                                                                unique_policy = {};
                                                                attempts = 1;
                                                                connection_attempts = 2;
                                                                rendezvous_hashing = {
                                                                  weights_file = "./controls/traffic_control.weights";
                                                                  bs_iva32_meta = {
                                                                    weight = 1.000;
                                                                    report = {
                                                                      uuid = "bs_iva32_meta";
                                                                      ranges = get_str_var("default_ranges");
                                                                      backend_time_ranges = "100ms,150ms,200ms,300ms,500ms";
                                                                      just_storage = false;
                                                                      disable_robotness = true;
                                                                      disable_sslness = true;
                                                                      events = {
                                                                        stats = "report";
                                                                      }; -- events
                                                                      balancer2 = {
                                                                        active_policy = {
                                                                          unique_policy = {};
                                                                        }; -- active_policy
                                                                        attempts = 1;
                                                                        rendezvous_hashing = {
                                                                          unpack(gen_proxy_backends({
                                                                            { "iva1-2603-31b-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:389b:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2604-adc-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:38aa:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2605-404-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3796:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2610-05d-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:38a8:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2614-221-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3898:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2618-99f-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:38a1:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2620-ccb-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3521:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2621-19d-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:351e:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2632-46b-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:362a:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2633-879-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3698:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2634-7ca-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3694:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2635-af8-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:36aa:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2637-2f2-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:38a2:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2641-e91-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3626:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2645-743-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:38a9:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2646-518-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:38a3:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2650-2a1-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3620:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2653-fdf-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:358c:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2655-fce-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3797:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2656-d0c-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:389f:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2657-777-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:38ad:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2661-b36-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:36a7:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2665-f36-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:351a:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2669-e9d-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3889:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2672-ed0-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3507:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2673-b22-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3789:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2674-a38-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3519:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2677-154-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3799:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2679-8d5-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:379b:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2680-ebc-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:35a4:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2681-382-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3794:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2682-41a-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:388b:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2684-c69-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:36a2:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2685-ca7-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:37ac:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2688-c7d-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:352d:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2690-22f-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3594:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2692-e79-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:359a:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2694-17e-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:389c:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2695-8d5-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3696:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2699-e84-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:35ae:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2700-10b-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:38af:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2704-d8a-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3784:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2705-097-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:388c:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2708-63c-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:379e:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2710-3a9-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3508:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2711-a56-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:37a3:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2712-657-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3787:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2714-378-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:37ae:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2715-f35-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:37a6:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2719-05b-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:350f:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2722-967-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:389e:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2725-274-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:35af:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2726-41b-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:369e:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2728-e62-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3514:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2733-fef-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:368b:10e:fc8e:0:4cfb"; };
                                                                            { "iva1-2734-761-msk-iva-yabs-fro-2f1-19707.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3788:10e:fc8e:0:4cfb"; };
                                                                          }, {
                                                                            resolve_timeout = "10ms";
                                                                            connect_timeout = "50ms";
                                                                            backend_timeout = "10s";
                                                                            fail_on_5xx = true;
                                                                            http_backend = true;
                                                                            buffering = false;
                                                                            keepalive_count = 1;
                                                                            need_resolve = true;
                                                                            keepalive_timeout = "60s";
                                                                          }))
                                                                        }; -- rendezvous_hashing
                                                                      }; -- balancer2
                                                                    }; -- report
                                                                  }; -- bs_iva32_meta
                                                                  bs_iva_experiment1 = {
                                                                    weight = 1.000;
                                                                    report = {
                                                                      uuid = "bs_iva_experiment1";
                                                                      ranges = get_str_var("default_ranges");
                                                                      backend_time_ranges = "100ms,150ms,200ms,300ms,500ms";
                                                                      just_storage = false;
                                                                      disable_robotness = true;
                                                                      disable_sslness = true;
                                                                      events = {
                                                                        stats = "report";
                                                                      }; -- events
                                                                      balancer2 = {
                                                                        active_policy = {
                                                                          unique_policy = {};
                                                                        }; -- active_policy
                                                                        attempts = 1;
                                                                        rendezvous_hashing = {
                                                                          unpack(gen_proxy_backends({
                                                                            { "iva1-3408-4a0-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4616:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3411-e18-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:36a0:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3413-2b9-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4424:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3415-b90-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4410:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3420-2fe-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4625:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3424-deb-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4622:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3426-035-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:460f:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3439-d65-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4519:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3444-54f-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:452c:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3525-863-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4fa9:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3529-bba-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4e8f:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3530-2e8-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4e8b:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3531-51f-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4e9f:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3533-717-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4ea2:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3534-c99-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4ea6:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3535-50a-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4e89:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3559-d8e-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4fa1:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3563-912-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4faf:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3571-dfc-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4fa3:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3573-43d-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4fa0:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3576-b11-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4fae:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3580-aad-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4e95:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3592-6c2-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4e84:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3606-8c7-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4ea3:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3614-ac7-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4e86:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3616-acc-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4fa2:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3619-6c3-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4fa8:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3624-fac-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4ea4:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3633-071-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4e96:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3639-ece-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4ea1:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3641-551-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4e87:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3642-217-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4f98:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3653-719-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4ea9:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3654-277-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4e9e:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3703-e8f-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4e98:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3722-2d3-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4e85:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3905-9f9-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:442c:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3908-109-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:4417:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3910-2b0-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:442f:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3921-ae8-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:362e:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3927-17d-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:441f:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3939-d1f-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:3624:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3949-d82-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:3606:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3952-76a-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:442a:10d:56ea:0:5ff5"; };
                                                                            { "iva1-3957-482-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:360e:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4192-84e-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5217:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4215-5e5-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5207:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4227-4fb-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5205:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4243-e1d-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5204:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4271-82f-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:522b:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4278-654-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5211:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4300-7ae-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5206:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4303-347-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:520f:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4317-3a1-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:529f:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4322-d6c-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:52a5:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4327-b8a-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:52ad:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4328-187-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:52af:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4329-1a1-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:52a2:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4332-901-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5288:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4335-b9a-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5298:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4336-411-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:52a0:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4337-f72-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5291:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4339-101-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:529e:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4341-fb1-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:529c:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4343-133-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:52a6:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4601-b0d-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:529b:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4607-82b-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5297:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4609-c04-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5282:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4656-05b-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:528b:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4679-b36-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5289:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4725-ff2-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5890:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4726-aaf-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:582f:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4728-6f9-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5886:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4729-61e-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:59a3:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4730-842-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5994:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4732-77a-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5990:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4733-8e8-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5823:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4734-222-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5085:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4737-52c-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5897:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4738-966-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5817:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4740-d34-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5082:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4743-1c0-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5ea3:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4744-779-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:588e:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4745-f3b-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5eab:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4746-ddd-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5e9e:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4747-61d-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5e8b:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4748-c49-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:582d:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4749-c5d-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:599a:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4750-fd5-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5889:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4751-b86-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5eac:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4794-208-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:57a6:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4803-af7-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:528c:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4804-d4e-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5292:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4805-bda-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:52ae:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4809-f27-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5287:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4815-536-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:528f:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4829-877-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:52ac:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4832-ccc-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:52ab:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4848-0ac-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5294:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4851-5a5-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5299:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4854-b26-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:52a9:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4867-215-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:529a:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4868-3c2-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5286:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4912-1d9-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:528e:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4914-15b-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:52a7:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4939-c71-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5293:10d:56ea:0:5ff5"; };
                                                                            { "iva1-4943-6e4-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:52a3:10d:56ea:0:5ff5"; };
                                                                            { "iva1-5035-f1f-msk-iva-yabs-fro-331-24565.gencfg-c.yandex.net"; 8001; 1000.000; "2a02:6b8:c0c:5296:10d:56ea:0:5ff5"; };
                                                                          }, {
                                                                            resolve_timeout = "10ms";
                                                                            connect_timeout = "50ms";
                                                                            backend_timeout = "10s";
                                                                            fail_on_5xx = true;
                                                                            http_backend = true;
                                                                            buffering = false;
                                                                            keepalive_count = 1;
                                                                            need_resolve = true;
                                                                            keepalive_timeout = "60s";
                                                                          }))
                                                                        }; -- rendezvous_hashing
                                                                      }; -- balancer2
                                                                    }; -- report
                                                                  }; -- bs_iva_experiment1
                                                                  bs_iva_newruntime1 = {
                                                                    weight = 1.000;
                                                                    report = {
                                                                      uuid = "bs_iva_newruntime1";
                                                                      ranges = get_str_var("default_ranges");
                                                                      backend_time_ranges = "100ms,150ms,200ms,300ms,500ms";
                                                                      just_storage = false;
                                                                      disable_robotness = true;
                                                                      disable_sslness = true;
                                                                      events = {
                                                                        stats = "report";
                                                                      }; -- events
                                                                      balancer2 = {
                                                                        active_policy = {
                                                                          unique_policy = {};
                                                                        }; -- active_policy
                                                                        attempts = 1;
                                                                        rendezvous_hashing = {
                                                                          unpack(gen_proxy_backends({
                                                                            { "iva1-3881-e0a-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:381d:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3882-1cd-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3699:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3883-60e-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:36af:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3884-158-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3525:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3885-106-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:37ab:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3886-f62-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3520:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3887-0f0-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3812:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3888-883-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3595:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3889-ff1-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:36a4:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3891-fe6-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3596:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3892-9ca-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:36a3:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3894-8e4-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3528:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3895-3f2-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3518:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3896-27d-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:350c:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3898-98b-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:37af:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3901-108-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3529:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3903-377-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:358f:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3913-c5a-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3590:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3916-d09-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3513:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3925-e44-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3585:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3928-4d0-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:358b:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3936-e1b-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3693:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3940-42c-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:369a:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3941-f7e-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3687:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3944-75a-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3691:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3947-203-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3695:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3948-e07-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3511:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3950-e8c-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:358a:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3953-24b-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:352c:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-3956-b0d-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3690:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-4197-43d-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:520b:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-4203-eba-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:520d:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-4207-1e3-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5215:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-4221-616-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5223:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-4225-ac9-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:520c:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-4228-1d8-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5200:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-4236-cec-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:522e:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-4249-3b8-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5214:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-4261-795-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5227:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-4269-f5e-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:521a:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-4277-e23-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5208:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-4284-e9f-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:522f:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-4287-20a-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:522c:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-4301-b06-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5203:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-4471-b23-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5707:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-4473-3ca-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5709:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-4477-b1d-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:570b:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-4497-fc0-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:572c:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-4515-ea7-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:570d:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-4520-3b5-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:570f:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-4603-838-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5895:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-4613-ced-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5989:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-4615-778-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:59a7:10e:fa8c:0:6e5f"; };
                                                                            { "iva1-4616-371-msk-iva-yabs-fro-c37-28255.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:588d:10e:fa8c:0:6e5f"; };
                                                                          }, {
                                                                            resolve_timeout = "10ms";
                                                                            connect_timeout = "50ms";
                                                                            backend_timeout = "10s";
                                                                            fail_on_5xx = true;
                                                                            http_backend = true;
                                                                            buffering = false;
                                                                            keepalive_count = 1;
                                                                            need_resolve = true;
                                                                            keepalive_timeout = "60s";
                                                                          }))
                                                                        }; -- rendezvous_hashing
                                                                      }; -- balancer2
                                                                    }; -- report
                                                                  }; -- bs_iva_newruntime1
                                                                  bs_iva_prestable1 = {
                                                                    weight = 1.000;
                                                                    report = {
                                                                      uuid = "bs_iva_prestable1";
                                                                      ranges = get_str_var("default_ranges");
                                                                      backend_time_ranges = "100ms,150ms,200ms,300ms,500ms";
                                                                      just_storage = false;
                                                                      disable_robotness = true;
                                                                      disable_sslness = true;
                                                                      events = {
                                                                        stats = "report";
                                                                      }; -- events
                                                                      balancer2 = {
                                                                        active_policy = {
                                                                          unique_policy = {};
                                                                        }; -- active_policy
                                                                        attempts = 1;
                                                                        rendezvous_hashing = {
                                                                          unpack(gen_proxy_backends({
                                                                            { "iva1-2599-f8b-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:380e:10d:5706:0:3f27"; };
                                                                            { "iva1-2607-b20-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3808:10d:5706:0:3f27"; };
                                                                            { "iva1-2612-0e0-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:381e:10d:5706:0:3f27"; };
                                                                            { "iva1-2624-b51-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3829:10d:5706:0:3f27"; };
                                                                            { "iva1-2639-11c-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3818:10d:5706:0:3f27"; };
                                                                            { "iva1-2647-da1-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3819:10d:5706:0:3f27"; };
                                                                            { "iva1-2648-5d9-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:381c:10d:5706:0:3f27"; };
                                                                            { "iva1-2652-a04-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:381b:10d:5706:0:3f27"; };
                                                                            { "iva1-2658-2cb-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3822:10d:5706:0:3f27"; };
                                                                            { "iva1-2659-025-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:380f:10d:5706:0:3f27"; };
                                                                            { "iva1-2660-47d-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3817:10d:5706:0:3f27"; };
                                                                            { "iva1-2676-359-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3825:10d:5706:0:3f27"; };
                                                                            { "iva1-2678-96d-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3816:10d:5706:0:3f27"; };
                                                                            { "iva1-2686-c94-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:382d:10d:5706:0:3f27"; };
                                                                            { "iva1-2691-a01-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3810:10d:5706:0:3f27"; };
                                                                            { "iva1-2706-9a7-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:382a:10d:5706:0:3f27"; };
                                                                            { "iva1-2713-cb7-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:381f:10d:5706:0:3f27"; };
                                                                            { "iva1-2718-e5f-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:382b:10d:5706:0:3f27"; };
                                                                            { "iva1-2723-a2c-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:381a:10d:5706:0:3f27"; };
                                                                            { "iva1-2724-81d-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:380c:10d:5706:0:3f27"; };
                                                                            { "iva1-2727-0e3-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3804:10d:5706:0:3f27"; };
                                                                            { "iva1-2730-241-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3806:10d:5706:0:3f27"; };
                                                                            { "iva1-2737-151-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3811:10d:5706:0:3f27"; };
                                                                            { "iva1-2741-884-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3828:10d:5706:0:3f27"; };
                                                                            { "iva1-2763-e4f-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3821:10d:5706:0:3f27"; };
                                                                            { "iva1-2769-dd2-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3823:10d:5706:0:3f27"; };
                                                                            { "iva1-2771-0bf-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:382e:10d:5706:0:3f27"; };
                                                                            { "iva1-2775-42f-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3824:10d:5706:0:3f27"; };
                                                                            { "iva1-2778-422-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3826:10d:5706:0:3f27"; };
                                                                            { "iva1-2783-e6d-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:382f:10d:5706:0:3f27"; };
                                                                            { "iva1-2800-7b6-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3815:10d:5706:0:3f27"; };
                                                                            { "iva1-2810-e40-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:382c:10d:5706:0:3f27"; };
                                                                            { "iva1-2826-78c-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:380a:10d:5706:0:3f27"; };
                                                                            { "iva1-2836-570-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3813:10d:5706:0:3f27"; };
                                                                            { "iva1-2838-0bd-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3827:10d:5706:0:3f27"; };
                                                                            { "iva1-2841-ac5-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3517:10d:5706:0:3f27"; };
                                                                            { "iva1-2843-291-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3894:10d:5706:0:3f27"; };
                                                                            { "iva1-2848-b44-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:369f:10d:5706:0:3f27"; };
                                                                            { "iva1-2849-e93-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3820:10d:5706:0:3f27"; };
                                                                            { "iva1-2851-0e0-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:361e:10d:5706:0:3f27"; };
                                                                            { "iva1-2854-892-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:441a:10d:5706:0:3f27"; };
                                                                            { "iva1-2855-fb3-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:37a5:10d:5706:0:3f27"; };
                                                                            { "iva1-2859-296-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:350d:10d:5706:0:3f27"; };
                                                                            { "iva1-2865-5bf-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:359b:10d:5706:0:3f27"; };
                                                                            { "iva1-2866-a69-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3814:10d:5706:0:3f27"; };
                                                                            { "iva1-2868-048-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:37a7:10d:5706:0:3f27"; };
                                                                            { "iva1-2870-7f5-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:36a8:10d:5706:0:3f27"; };
                                                                            { "iva1-2872-a8c-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4416:10d:5706:0:3f27"; };
                                                                            { "iva1-2873-ec5-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4613:10d:5706:0:3f27"; };
                                                                            { "iva1-2884-d58-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:358d:10d:5706:0:3f27"; };
                                                                            { "iva1-2888-14a-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:461c:10d:5706:0:3f27"; };
                                                                            { "iva1-2891-1cf-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:462c:10d:5706:0:3f27"; };
                                                                            { "iva1-2902-ec4-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:351b:10d:5706:0:3f27"; };
                                                                            { "iva1-2904-64b-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4614:10d:5706:0:3f27"; };
                                                                            { "iva1-2911-8b7-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4620:10d:5706:0:3f27"; };
                                                                            { "iva1-2927-9db-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:440d:10d:5706:0:3f27"; };
                                                                            { "iva1-2929-d28-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4618:10d:5706:0:3f27"; };
                                                                            { "iva1-2931-3ef-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:360d:10d:5706:0:3f27"; };
                                                                            { "iva1-2934-0c7-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4624:10d:5706:0:3f27"; };
                                                                            { "iva1-2937-df4-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:440f:10d:5706:0:3f27"; };
                                                                            { "iva1-2945-653-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:462b:10d:5706:0:3f27"; };
                                                                            { "iva1-2950-e55-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:441e:10d:5706:0:3f27"; };
                                                                            { "iva1-2963-8b4-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4626:10d:5706:0:3f27"; };
                                                                            { "iva1-2966-1ad-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:461f:10d:5706:0:3f27"; };
                                                                            { "iva1-2968-4a4-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3686:10d:5706:0:3f27"; };
                                                                            { "iva1-2976-aaa-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:460d:10d:5706:0:3f27"; };
                                                                            { "iva1-2978-9f1-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:458d:10d:5706:0:3f27"; };
                                                                            { "iva1-2985-3a4-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4610:10d:5706:0:3f27"; };
                                                                            { "iva1-2988-fec-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4604:10d:5706:0:3f27"; };
                                                                            { "iva1-2990-42c-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:440b:10d:5706:0:3f27"; };
                                                                            { "iva1-2993-cb6-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3607:10d:5706:0:3f27"; };
                                                                            { "iva1-2994-2f7-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3923:10d:5706:0:3f27"; };
                                                                            { "iva1-3081-e35-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:450f:10d:5706:0:3f27"; };
                                                                            { "iva1-3083-a1c-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4515:10d:5706:0:3f27"; };
                                                                            { "iva1-3085-f4d-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4513:10d:5706:0:3f27"; };
                                                                            { "iva1-3086-a25-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4524:10d:5706:0:3f27"; };
                                                                            { "iva1-3087-15e-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4503:10d:5706:0:3f27"; };
                                                                            { "iva1-3088-1f7-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4522:10d:5706:0:3f27"; };
                                                                            { "iva1-3092-894-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4517:10d:5706:0:3f27"; };
                                                                            { "iva1-3093-5d5-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4527:10d:5706:0:3f27"; };
                                                                            { "iva1-3100-0f4-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:499a:10d:5706:0:3f27"; };
                                                                            { "iva1-3168-393-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:36ab:10d:5706:0:3f27"; };
                                                                            { "iva1-3176-52a-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4409:10d:5706:0:3f27"; };
                                                                            { "iva1-3179-82c-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:450b:10d:5706:0:3f27"; };
                                                                            { "iva1-3193-28f-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:36ad:10d:5706:0:3f27"; };
                                                                            { "iva1-3291-346-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:3625:10d:5706:0:3f27"; };
                                                                            { "iva1-3327-ea5-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:361b:10d:5706:0:3f27"; };
                                                                            { "iva1-3335-7d1-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4b9c:10d:5706:0:3f27"; };
                                                                            { "iva1-3340-bc8-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4b99:10d:5706:0:3f27"; };
                                                                            { "iva1-3344-98e-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4ba7:10d:5706:0:3f27"; };
                                                                            { "iva1-3345-410-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4ba9:10d:5706:0:3f27"; };
                                                                            { "iva1-3347-eac-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4baf:10d:5706:0:3f27"; };
                                                                            { "iva1-3350-b7f-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4ba4:10d:5706:0:3f27"; };
                                                                            { "iva1-3352-9a2-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4b89:10d:5706:0:3f27"; };
                                                                            { "iva1-3354-32f-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4bad:10d:5706:0:3f27"; };
                                                                            { "iva1-3357-eec-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4b8b:10d:5706:0:3f27"; };
                                                                            { "iva1-3360-fc2-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4ba5:10d:5706:0:3f27"; };
                                                                            { "iva1-3363-4cb-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4b98:10d:5706:0:3f27"; };
                                                                            { "iva1-3364-aa2-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4bab:10d:5706:0:3f27"; };
                                                                            { "iva1-3367-0de-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4b84:10d:5706:0:3f27"; };
                                                                            { "iva1-3376-1e2-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4ba2:10d:5706:0:3f27"; };
                                                                            { "iva1-3377-55e-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4b88:10d:5706:0:3f27"; };
                                                                            { "iva1-3378-789-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4b9d:10d:5706:0:3f27"; };
                                                                            { "iva1-3383-f05-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4ba1:10d:5706:0:3f27"; };
                                                                            { "iva1-3392-ffc-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4526:10d:5706:0:3f27"; };
                                                                            { "iva1-3397-3aa-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:460e:10d:5706:0:3f27"; };
                                                                            { "iva1-3400-e4e-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4511:10d:5706:0:3f27"; };
                                                                            { "iva1-3401-e87-msk-iva-yabs-fro-3a8-16167.gencfg-c.yandex.net"; 8001; 800.000; "2a02:6b8:c0c:4523:10d:5706:0:3f27"; };
                                                                          }, {
                                                                            resolve_timeout = "10ms";
                                                                            connect_timeout = "50ms";
                                                                            backend_timeout = "10s";
                                                                            fail_on_5xx = true;
                                                                            http_backend = true;
                                                                            buffering = false;
                                                                            keepalive_count = 1;
                                                                            need_resolve = true;
                                                                            keepalive_timeout = "60s";
                                                                          }))
                                                                        }; -- rendezvous_hashing
                                                                      }; -- balancer2
                                                                    }; -- report
                                                                  }; -- bs_iva_prestable1
                                                                  bs_iva_stable1 = {
                                                                    weight = 1.000;
                                                                    report = {
                                                                      uuid = "bs_iva_stable1";
                                                                      ranges = get_str_var("default_ranges");
                                                                      backend_time_ranges = "100ms,150ms,200ms,300ms,500ms";
                                                                      just_storage = false;
                                                                      disable_robotness = true;
                                                                      disable_sslness = true;
                                                                      events = {
                                                                        stats = "report";
                                                                      }; -- events
                                                                      balancer2 = {
                                                                        active_policy = {
                                                                          unique_policy = {};
                                                                        }; -- active_policy
                                                                        attempts = 1;
                                                                        rendezvous_hashing = {
                                                                          unpack(gen_proxy_backends({
                                                                            { "iva1-2595-a38-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:352a:10d:56e9:0:5c92"; };
                                                                            { "iva1-2596-fe2-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3522:10d:56e9:0:5c92"; };
                                                                            { "iva1-2597-4a1-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:37ad:10d:56e9:0:5c92"; };
                                                                            { "iva1-2608-ec6-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:361c:10d:56e9:0:5c92"; };
                                                                            { "iva1-2616-8a9-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3697:10d:56e9:0:5c92"; };
                                                                            { "iva1-2625-e64-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3611:10d:56e9:0:5c92"; };
                                                                            { "iva1-2626-ff1-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:35ad:10d:56e9:0:5c92"; };
                                                                            { "iva1-2627-bb5-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3506:10d:56e9:0:5c92"; };
                                                                            { "iva1-2630-378-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3589:10d:56e9:0:5c92"; };
                                                                            { "iva1-2631-0f3-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:36a1:10d:56e9:0:5c92"; };
                                                                            { "iva1-2636-b73-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3892:10d:56e9:0:5c92"; };
                                                                            { "iva1-2638-cf9-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:36ac:10d:56e9:0:5c92"; };
                                                                            { "iva1-2640-07e-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3618:10d:56e9:0:5c92"; };
                                                                            { "iva1-2642-ab0-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:35ac:10d:56e9:0:5c92"; };
                                                                            { "iva1-2649-46f-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3598:10d:56e9:0:5c92"; };
                                                                            { "iva1-2651-11b-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:378a:10d:56e9:0:5c92"; };
                                                                            { "iva1-2654-450-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3597:10d:56e9:0:5c92"; };
                                                                            { "iva1-2662-dd8-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:388f:10d:56e9:0:5c92"; };
                                                                            { "iva1-2667-a57-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3899:10d:56e9:0:5c92"; };
                                                                            { "iva1-2670-1e2-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3893:10d:56e9:0:5c92"; };
                                                                            { "iva1-2671-eb0-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3510:10d:56e9:0:5c92"; };
                                                                            { "iva1-2675-739-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:35a2:10d:56e9:0:5c92"; };
                                                                            { "iva1-2693-c04-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3890:10d:56e9:0:5c92"; };
                                                                            { "iva1-2696-0bf-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:37a1:10d:56e9:0:5c92"; };
                                                                            { "iva1-2698-f7a-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3509:10d:56e9:0:5c92"; };
                                                                            { "iva1-2701-aa6-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:36ae:10d:56e9:0:5c92"; };
                                                                            { "iva1-2703-6bc-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3793:10d:56e9:0:5c92"; };
                                                                            { "iva1-2707-2fa-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3888:10d:56e9:0:5c92"; };
                                                                            { "iva1-2709-54d-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:35a8:10d:56e9:0:5c92"; };
                                                                            { "iva1-2716-8a5-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3792:10d:56e9:0:5c92"; };
                                                                            { "iva1-2720-52e-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:379d:10d:56e9:0:5c92"; };
                                                                            { "iva1-2721-db0-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3587:10d:56e9:0:5c92"; };
                                                                            { "iva1-2729-075-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:37a2:10d:56e9:0:5c92"; };
                                                                            { "iva1-2735-5cf-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3628:10d:56e9:0:5c92"; };
                                                                            { "iva1-2738-5bd-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:38a4:10d:56e9:0:5c92"; };
                                                                            { "iva1-2749-c02-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:360b:10d:56e9:0:5c92"; };
                                                                            { "iva1-2752-04a-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:351c:10d:56e9:0:5c92"; };
                                                                            { "iva1-2756-5a7-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3527:10d:56e9:0:5c92"; };
                                                                            { "iva1-2760-5bc-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3795:10d:56e9:0:5c92"; };
                                                                            { "iva1-2772-2dd-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3612:10d:56e9:0:5c92"; };
                                                                            { "iva1-2773-fca-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:36a6:10d:56e9:0:5c92"; };
                                                                            { "iva1-2774-c6c-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3790:10d:56e9:0:5c92"; };
                                                                            { "iva1-2779-dba-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:38a6:10d:56e9:0:5c92"; };
                                                                            { "iva1-2786-2db-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3786:10d:56e9:0:5c92"; };
                                                                            { "iva1-2788-9e4-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:3895:10d:56e9:0:5c92"; };
                                                                            { "iva1-2789-4ea-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:362c:10d:56e9:0:5c92"; };
                                                                            { "iva1-2790-36a-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:36a5:10d:56e9:0:5c92"; };
                                                                            { "iva1-2804-ce8-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:359d:10d:56e9:0:5c92"; };
                                                                            { "iva1-2806-3ba-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:38ac:10d:56e9:0:5c92"; };
                                                                            { "iva1-2815-712-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:379a:10d:56e9:0:5c92"; };
                                                                            { "iva1-2824-65f-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:369b:10d:56e9:0:5c92"; };
                                                                            { "iva1-2825-539-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:350a:10d:56e9:0:5c92"; };
                                                                            { "iva1-2827-828-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:360c:10d:56e9:0:5c92"; };
                                                                            { "iva1-2833-9b1-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:37a4:10d:56e9:0:5c92"; };
                                                                            { "iva1-3265-d79-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:4b9e:10d:56e9:0:5c92"; };
                                                                            { "iva1-3270-ea4-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:4b91:10d:56e9:0:5c92"; };
                                                                            { "iva1-3272-a3d-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:4ba6:10d:56e9:0:5c92"; };
                                                                            { "iva1-3278-1fa-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:4b8a:10d:56e9:0:5c92"; };
                                                                            { "iva1-3280-922-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:4b97:10d:56e9:0:5c92"; };
                                                                            { "iva1-3285-061-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:4baa:10d:56e9:0:5c92"; };
                                                                            { "iva1-3286-635-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:4b9b:10d:56e9:0:5c92"; };
                                                                            { "iva1-3287-2c0-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:4b95:10d:56e9:0:5c92"; };
                                                                            { "iva1-3296-384-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:4ba0:10d:56e9:0:5c92"; };
                                                                            { "iva1-3297-ce6-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:4b8f:10d:56e9:0:5c92"; };
                                                                            { "iva1-3299-935-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:4b9a:10d:56e9:0:5c92"; };
                                                                            { "iva1-3301-991-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:4b96:10d:56e9:0:5c92"; };
                                                                            { "iva1-3302-311-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:4b9f:10d:56e9:0:5c92"; };
                                                                            { "iva1-3304-77a-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:4b8d:10d:56e9:0:5c92"; };
                                                                            { "iva1-3309-fb7-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:4b90:10d:56e9:0:5c92"; };
                                                                            { "iva1-3314-ae1-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:4b87:10d:56e9:0:5c92"; };
                                                                            { "iva1-3316-019-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:4b93:10d:56e9:0:5c92"; };
                                                                            { "iva1-3319-fcc-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:4b8c:10d:56e9:0:5c92"; };
                                                                            { "iva1-3320-704-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:4bae:10d:56e9:0:5c92"; };
                                                                            { "iva1-4318-02a-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:532b:10d:56e9:0:5c92"; };
                                                                            { "iva1-4319-7af-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5329:10d:56e9:0:5c92"; };
                                                                            { "iva1-4320-693-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:531f:10d:56e9:0:5c92"; };
                                                                            { "iva1-4321-3df-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5309:10d:56e9:0:5c92"; };
                                                                            { "iva1-4323-b38-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:532e:10d:56e9:0:5c92"; };
                                                                            { "iva1-4324-3ee-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5323:10d:56e9:0:5c92"; };
                                                                            { "iva1-4326-8d9-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:531c:10d:56e9:0:5c92"; };
                                                                            { "iva1-4330-ee9-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5311:10d:56e9:0:5c92"; };
                                                                            { "iva1-4331-59d-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5319:10d:56e9:0:5c92"; };
                                                                            { "iva1-4333-21f-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:531a:10d:56e9:0:5c92"; };
                                                                            { "iva1-4334-051-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5308:10d:56e9:0:5c92"; };
                                                                            { "iva1-4338-0be-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:532c:10d:56e9:0:5c92"; };
                                                                            { "iva1-4342-18f-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5304:10d:56e9:0:5c92"; };
                                                                            { "iva1-4684-2f8-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:532f:10d:56e9:0:5c92"; };
                                                                            { "iva1-4685-5e1-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5327:10d:56e9:0:5c92"; };
                                                                            { "iva1-4763-0ba-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:530b:10d:56e9:0:5c92"; };
                                                                            { "iva1-4767-1bc-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:532d:10d:56e9:0:5c92"; };
                                                                            { "iva1-4790-e1e-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5313:10d:56e9:0:5c92"; };
                                                                            { "iva1-4792-078-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:531b:10d:56e9:0:5c92"; };
                                                                            { "iva1-4806-9e0-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5324:10d:56e9:0:5c92"; };
                                                                            { "iva1-4816-a27-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5315:10d:56e9:0:5c92"; };
                                                                            { "iva1-4826-b0b-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5316:10d:56e9:0:5c92"; };
                                                                            { "iva1-4839-157-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5326:10d:56e9:0:5c92"; };
                                                                            { "iva1-4847-70e-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5325:10d:56e9:0:5c92"; };
                                                                            { "iva1-4870-418-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:530e:10d:56e9:0:5c92"; };
                                                                            { "iva1-4895-ce4-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5328:10d:56e9:0:5c92"; };
                                                                            { "iva1-4903-d1e-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5317:10d:56e9:0:5c92"; };
                                                                            { "iva1-5032-32e-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:530d:10d:56e9:0:5c92"; };
                                                                            { "iva1-5034-ea9-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:532a:10d:56e9:0:5c92"; };
                                                                            { "iva1-5041-ddb-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5314:10d:56e9:0:5c92"; };
                                                                            { "iva1-5147-05f-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5322:10d:56e9:0:5c92"; };
                                                                            { "iva1-5149-af8-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5312:10d:56e9:0:5c92"; };
                                                                            { "iva1-5163-56d-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:5320:10d:56e9:0:5c92"; };
                                                                            { "iva1-5169-153-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:531e:10d:56e9:0:5c92"; };
                                                                            { "iva1-5170-afb-msk-iva-yabs-fro-67e-23698.gencfg-c.yandex.net"; 8001; 1130.000; "2a02:6b8:c0c:530f:10d:56e9:0:5c92"; };
                                                                          }, {
                                                                            resolve_timeout = "10ms";
                                                                            connect_timeout = "50ms";
                                                                            backend_timeout = "10s";
                                                                            fail_on_5xx = true;
                                                                            http_backend = true;
                                                                            buffering = false;
                                                                            keepalive_count = 1;
                                                                            need_resolve = true;
                                                                            keepalive_timeout = "60s";
                                                                          }))
                                                                        }; -- rendezvous_hashing
                                                                      }; -- balancer2
                                                                    }; -- report
                                                                  }; -- bs_iva_stable1
                                                                  bs_man56_meta = {
                                                                    weight = 1.000;
                                                                    report = {
                                                                      uuid = "bs_man56_meta";
                                                                      ranges = get_str_var("default_ranges");
                                                                      backend_time_ranges = "100ms,150ms,200ms,300ms,500ms";
                                                                      just_storage = false;
                                                                      disable_robotness = true;
                                                                      disable_sslness = true;
                                                                      events = {
                                                                        stats = "report";
                                                                      }; -- events
                                                                      balancer2 = {
                                                                        active_policy = {
                                                                          unique_policy = {};
                                                                        }; -- active_policy
                                                                        attempts = 1;
                                                                        rendezvous_hashing = {
                                                                          unpack(gen_proxy_backends({
                                                                            { "man2-2728-bbc-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b8f:10e:fc96:0:535c"; };
                                                                            { "man2-2729-e2a-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b97:10e:fc96:0:535c"; };
                                                                            { "man2-2730-e96-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:220c:10e:fc96:0:535c"; };
                                                                            { "man2-2732-dcc-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:ba0:10e:fc96:0:535c"; };
                                                                            { "man2-2733-7c7-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2205:10e:fc96:0:535c"; };
                                                                            { "man2-2734-9b6-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2206:10e:fc96:0:535c"; };
                                                                            { "man2-2736-34c-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:221b:10e:fc96:0:535c"; };
                                                                            { "man2-2737-a11-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b9a:10e:fc96:0:535c"; };
                                                                            { "man2-2738-a79-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:511:10e:fc96:0:535c"; };
                                                                            { "man2-2739-cf6-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2221:10e:fc96:0:535c"; };
                                                                            { "man2-2740-377-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:220d:10e:fc96:0:535c"; };
                                                                            { "man2-2741-69d-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:914:10e:fc96:0:535c"; };
                                                                            { "man2-2742-e54-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:91b:10e:fc96:0:535c"; };
                                                                            { "man2-2743-d8d-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:51e:10e:fc96:0:535c"; };
                                                                            { "man2-2744-7d3-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:906:10e:fc96:0:535c"; };
                                                                            { "man2-2745-72d-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:91d:10e:fc96:0:535c"; };
                                                                            { "man2-2746-e7c-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2213:10e:fc96:0:535c"; };
                                                                            { "man2-2749-7b1-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:50e:10e:fc96:0:535c"; };
                                                                            { "man2-2750-6b4-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:910:10e:fc96:0:535c"; };
                                                                            { "man2-2751-f85-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:909:10e:fc96:0:535c"; };
                                                                            { "man2-2752-b1b-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:903:10e:fc96:0:535c"; };
                                                                            { "man2-2753-58d-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:902:10e:fc96:0:535c"; };
                                                                            { "man2-2755-220-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:508:10e:fc96:0:535c"; };
                                                                            { "man2-2756-242-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:221a:10e:fc96:0:535c"; };
                                                                            { "man2-2757-5b5-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:908:10e:fc96:0:535c"; };
                                                                            { "man2-2759-815-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2201:10e:fc96:0:535c"; };
                                                                            { "man2-2760-8b3-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:91c:10e:fc96:0:535c"; };
                                                                            { "man2-2761-dbd-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:907:10e:fc96:0:535c"; };
                                                                            { "man2-2762-4ff-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2211:10e:fc96:0:535c"; };
                                                                            { "man2-2763-d48-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2207:10e:fc96:0:535c"; };
                                                                            { "man2-2764-f54-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2228:10e:fc96:0:535c"; };
                                                                            { "man2-2765-f56-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:919:10e:fc96:0:535c"; };
                                                                            { "man2-2766-531-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:911:10e:fc96:0:535c"; };
                                                                            { "man2-2767-0e8-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:ba1:10e:fc96:0:535c"; };
                                                                            { "man2-2768-985-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:913:10e:fc96:0:535c"; };
                                                                            { "man2-2769-551-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:220a:10e:fc96:0:535c"; };
                                                                            { "man2-2772-866-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2208:10e:fc96:0:535c"; };
                                                                            { "man2-2774-07b-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1517:10e:fc96:0:535c"; };
                                                                            { "man2-2776-119-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:516:10e:fc96:0:535c"; };
                                                                            { "man2-2778-c7b-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1514:10e:fc96:0:535c"; };
                                                                            { "man2-2779-8aa-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:506:10e:fc96:0:535c"; };
                                                                            { "man2-2781-2a1-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:537:10e:fc96:0:535c"; };
                                                                            { "man2-2782-fde-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:52e:10e:fc96:0:535c"; };
                                                                            { "man2-2783-402-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:52c:10e:fc96:0:535c"; };
                                                                            { "man2-2785-112-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1516:10e:fc96:0:535c"; };
                                                                            { "man2-2788-e44-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1503:10e:fc96:0:535c"; };
                                                                            { "man2-2798-a9b-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:151d:10e:fc96:0:535c"; };
                                                                            { "man2-2799-c9e-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1510:10e:fc96:0:535c"; };
                                                                            { "man2-2800-b04-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1502:10e:fc96:0:535c"; };
                                                                            { "man2-2809-ef1-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1512:10e:fc96:0:535c"; };
                                                                            { "man2-2810-760-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1511:10e:fc96:0:535c"; };
                                                                            { "man2-2815-768-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1523:10e:fc96:0:535c"; };
                                                                            { "man2-2816-706-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:151c:10e:fc96:0:535c"; };
                                                                            { "man2-2818-cb6-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:151b:10e:fc96:0:535c"; };
                                                                            { "man2-2819-615-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:150a:10e:fc96:0:535c"; };
                                                                            { "man2-2820-903-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:152d:10e:fc96:0:535c"; };
                                                                            { "man2-2822-817-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1527:10e:fc96:0:535c"; };
                                                                            { "man2-2826-a3c-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:923:10e:fc96:0:535c"; };
                                                                            { "man2-2829-0fa-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:51d:10e:fc96:0:535c"; };
                                                                            { "man2-2830-1f7-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2202:10e:fc96:0:535c"; };
                                                                            { "man2-2834-f3d-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:917:10e:fc96:0:535c"; };
                                                                            { "man2-2835-41c-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:91f:10e:fc96:0:535c"; };
                                                                            { "man2-2836-caf-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b83:10e:fc96:0:535c"; };
                                                                            { "man2-2837-6ee-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:53c:10e:fc96:0:535c"; };
                                                                            { "man2-2838-914-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1520:10e:fc96:0:535c"; };
                                                                            { "man2-2839-405-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:904:10e:fc96:0:535c"; };
                                                                            { "man2-2841-64d-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:90a:10e:fc96:0:535c"; };
                                                                            { "man2-2842-520-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:bac:10e:fc96:0:535c"; };
                                                                            { "man2-2843-b4d-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2210:10e:fc96:0:535c"; };
                                                                            { "man2-2844-162-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:ba6:10e:fc96:0:535c"; };
                                                                            { "man2-2847-b67-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1515:10e:fc96:0:535c"; };
                                                                            { "man2-2849-ec6-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2218:10e:fc96:0:535c"; };
                                                                            { "man2-2850-7e7-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:912:10e:fc96:0:535c"; };
                                                                            { "man2-2853-c3c-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b84:10e:fc96:0:535c"; };
                                                                            { "man2-2855-c21-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:150b:10e:fc96:0:535c"; };
                                                                            { "man2-2857-5d1-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b9e:10e:fc96:0:535c"; };
                                                                            { "man2-2860-5eb-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b8c:10e:fc96:0:535c"; };
                                                                            { "man2-2861-433-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2220:10e:fc96:0:535c"; };
                                                                            { "man2-2865-6fd-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:926:10e:fc96:0:535c"; };
                                                                            { "man2-2866-981-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:90c:10e:fc96:0:535c"; };
                                                                            { "man2-2868-437-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1513:10e:fc96:0:535c"; };
                                                                            { "man2-2869-0e8-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b96:10e:fc96:0:535c"; };
                                                                            { "man2-2872-cad-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b99:10e:fc96:0:535c"; };
                                                                            { "man2-2873-957-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1519:10e:fc96:0:535c"; };
                                                                            { "man2-2874-dbd-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:905:10e:fc96:0:535c"; };
                                                                            { "man2-2875-e18-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:152c:10e:fc96:0:535c"; };
                                                                            { "man2-2877-4b9-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:222f:10e:fc96:0:535c"; };
                                                                            { "man2-2878-581-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:50d:10e:fc96:0:535c"; };
                                                                            { "man2-2879-035-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b9d:10e:fc96:0:535c"; };
                                                                            { "man2-2881-4be-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:514:10e:fc96:0:535c"; };
                                                                            { "man2-2882-5ab-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1522:10e:fc96:0:535c"; };
                                                                            { "man2-2883-a76-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2203:10e:fc96:0:535c"; };
                                                                            { "man2-2884-ce6-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:90d:10e:fc96:0:535c"; };
                                                                            { "man2-2885-ed3-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:ba2:10e:fc96:0:535c"; };
                                                                            { "man2-2888-2d8-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:916:10e:fc96:0:535c"; };
                                                                            { "man2-2889-de0-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:91e:10e:fc96:0:535c"; };
                                                                            { "man2-2891-970-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:524:10e:fc96:0:535c"; };
                                                                            { "man2-2892-0b9-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1524:10e:fc96:0:535c"; };
                                                                            { "man2-2894-470-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b98:10e:fc96:0:535c"; };
                                                                            { "man2-2895-f25-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b95:10e:fc96:0:535c"; };
                                                                            { "man2-2896-479-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:52f:10e:fc96:0:535c"; };
                                                                            { "man2-2901-598-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2219:10e:fc96:0:535c"; };
                                                                            { "man2-2902-a42-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:512:10e:fc96:0:535c"; };
                                                                            { "man2-2903-ecc-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1521:10e:fc96:0:535c"; };
                                                                            { "man2-2904-48f-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1518:10e:fc96:0:535c"; };
                                                                            { "man2-2907-81c-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:50a:10e:fc96:0:535c"; };
                                                                            { "man2-2909-7fc-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:51b:10e:fc96:0:535c"; };
                                                                            { "man2-2911-4d6-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1525:10e:fc96:0:535c"; };
                                                                            { "man2-2914-065-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b8a:10e:fc96:0:535c"; };
                                                                            { "man2-2915-cdd-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:925:10e:fc96:0:535c"; };
                                                                            { "man2-2916-472-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b87:10e:fc96:0:535c"; };
                                                                            { "man2-2917-0f4-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:90b:10e:fc96:0:535c"; };
                                                                            { "man2-2918-573-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1501:10e:fc96:0:535c"; };
                                                                            { "man2-2919-d5c-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:51c:10e:fc96:0:535c"; };
                                                                            { "man2-3244-318-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4c09:10e:fc96:0:535c"; };
                                                                            { "man2-3245-f1b-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4c13:10e:fc96:0:535c"; };
                                                                            { "man2-4052-988-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:320a:10e:fc96:0:535c"; };
                                                                            { "man2-4937-935-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:10ad:10e:fc96:0:535c"; };
                                                                            { "man2-4940-310-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1092:10e:fc96:0:535c"; };
                                                                            { "man2-4943-805-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1122:10e:fc96:0:535c"; };
                                                                            { "man2-4944-4ee-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1106:10e:fc96:0:535c"; };
                                                                            { "man2-4945-899-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:111c:10e:fc96:0:535c"; };
                                                                            { "man2-4947-c3e-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1413:10e:fc96:0:535c"; };
                                                                            { "man2-4953-6ea-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b01:10e:fc96:0:535c"; };
                                                                            { "man2-4954-b67-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1117:10e:fc96:0:535c"; };
                                                                            { "man2-4957-46b-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1406:10e:fc96:0:535c"; };
                                                                            { "man2-4962-011-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1426:10e:fc96:0:535c"; };
                                                                            { "man2-4963-951-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1099:10e:fc96:0:535c"; };
                                                                            { "man2-4968-9b3-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1126:10e:fc96:0:535c"; };
                                                                            { "man2-4976-f3b-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:111e:10e:fc96:0:535c"; };
                                                                            { "man2-4982-e12-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:10a2:10e:fc96:0:535c"; };
                                                                            { "man2-4986-9b8-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1109:10e:fc96:0:535c"; };
                                                                            { "man2-5018-f91-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1400:10e:fc96:0:535c"; };
                                                                            { "man2-5022-077-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:a06:10e:fc96:0:535c"; };
                                                                            { "man2-5026-438-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1424:10e:fc96:0:535c"; };
                                                                            { "man2-5027-ba4-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:a26:10e:fc96:0:535c"; };
                                                                            { "man2-5028-fb9-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:a20:10e:fc96:0:535c"; };
                                                                            { "man2-5029-8bc-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:f17:10e:fc96:0:535c"; };
                                                                            { "man2-5125-8f0-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1909:10e:fc96:0:535c"; };
                                                                            { "man2-5138-d4d-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:a24:10e:fc96:0:535c"; };
                                                                            { "man2-5144-356-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:f13:10e:fc96:0:535c"; };
                                                                            { "man2-5155-b18-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1112:10e:fc96:0:535c"; };
                                                                            { "man2-5157-0a2-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1127:10e:fc96:0:535c"; };
                                                                            { "man2-5163-162-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:141d:10e:fc96:0:535c"; };
                                                                            { "man2-5357-568-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:140e:10e:fc96:0:535c"; };
                                                                            { "man2-5395-d9c-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1088:10e:fc96:0:535c"; };
                                                                            { "man2-5397-c09-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1091:10e:fc96:0:535c"; };
                                                                            { "man2-5398-d5e-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:f09:10e:fc96:0:535c"; };
                                                                            { "man2-5403-01e-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:140c:10e:fc96:0:535c"; };
                                                                            { "man2-5413-d03-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:10ac:10e:fc96:0:535c"; };
                                                                            { "man2-5421-db2-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:a1d:10e:fc96:0:535c"; };
                                                                            { "man2-5431-e09-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:10a4:10e:fc96:0:535c"; };
                                                                            { "man2-5434-e34-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1081:10e:fc96:0:535c"; };
                                                                            { "man2-5443-d98-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:10a3:10e:fc96:0:535c"; };
                                                                            { "man2-5444-48d-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1089:10e:fc96:0:535c"; };
                                                                            { "man2-5448-6fc-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:f01:10e:fc96:0:535c"; };
                                                                            { "man2-5450-605-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b0f:10e:fc96:0:535c"; };
                                                                            { "man2-5451-bb5-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1421:10e:fc96:0:535c"; };
                                                                            { "man2-5453-778-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1096:10e:fc96:0:535c"; };
                                                                            { "man2-5468-0d4-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1082:10e:fc96:0:535c"; };
                                                                            { "man2-5482-f44-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:109d:10e:fc96:0:535c"; };
                                                                            { "man2-5483-e6f-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:140f:10e:fc96:0:535c"; };
                                                                            { "man2-5489-336-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:f1d:10e:fc96:0:535c"; };
                                                                            { "man2-5513-d5e-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1417:10e:fc96:0:535c"; };
                                                                            { "man2-5558-107-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:a13:10e:fc96:0:535c"; };
                                                                            { "man2-5619-5ed-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:a12:10e:fc96:0:535c"; };
                                                                            { "man2-5637-8b0-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:141e:10e:fc96:0:535c"; };
                                                                            { "man2-5673-f6d-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1408:10e:fc96:0:535c"; };
                                                                            { "man2-5675-a96-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1415:10e:fc96:0:535c"; };
                                                                            { "man2-5688-161-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:819:10e:fc96:0:535c"; };
                                                                            { "man2-5689-fbf-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:80a:10e:fc96:0:535c"; };
                                                                            { "man2-5690-08b-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:c08:10e:fc96:0:535c"; };
                                                                            { "man2-5692-0e9-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:c1c:10e:fc96:0:535c"; };
                                                                            { "man2-5697-b2c-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:88f:10e:fc96:0:535c"; };
                                                                            { "man2-5699-274-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:8a4:10e:fc96:0:535c"; };
                                                                            { "man2-5703-ed5-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:8a0:10e:fc96:0:535c"; };
                                                                            { "man2-5704-33d-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:881:10e:fc96:0:535c"; };
                                                                            { "man2-5706-a7f-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:8a5:10e:fc96:0:535c"; };
                                                                            { "man2-5711-348-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b22:10e:fc96:0:535c"; };
                                                                            { "man2-5715-22b-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:f21:10e:fc96:0:535c"; };
                                                                            { "man2-5719-148-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:f16:10e:fc96:0:535c"; };
                                                                            { "man2-5721-1f9-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:110b:10e:fc96:0:535c"; };
                                                                            { "man2-5722-0e1-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b08:10e:fc96:0:535c"; };
                                                                            { "man2-5724-ae0-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:f06:10e:fc96:0:535c"; };
                                                                            { "man2-5727-af9-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:821:10e:fc96:0:535c"; };
                                                                            { "man2-5734-b63-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:c11:10e:fc96:0:535c"; };
                                                                            { "man2-5735-bd3-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:811:10e:fc96:0:535c"; };
                                                                            { "man2-5738-b51-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:c0a:10e:fc96:0:535c"; };
                                                                            { "man2-5740-16c-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:c15:10e:fc96:0:535c"; };
                                                                            { "man2-5741-fc6-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b0a:10e:fc96:0:535c"; };
                                                                            { "man2-5753-3ca-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:c14:10e:fc96:0:535c"; };
                                                                            { "man2-5797-647-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:c1e:10e:fc96:0:535c"; };
                                                                            { "man2-5810-1dd-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:893:10e:fc96:0:535c"; };
                                                                            { "man2-5813-16b-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:892:10e:fc96:0:535c"; };
                                                                            { "man2-5821-c1f-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:c10:10e:fc96:0:535c"; };
                                                                            { "man2-5851-7a8-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1414:10e:fc96:0:535c"; };
                                                                            { "man2-5866-14e-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:f12:10e:fc96:0:535c"; };
                                                                            { "man2-5867-78d-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:822:10e:fc96:0:535c"; };
                                                                            { "man2-5890-7ea-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:89e:10e:fc96:0:535c"; };
                                                                            { "man2-5894-736-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1105:10e:fc96:0:535c"; };
                                                                            { "man2-5923-e17-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:885:10e:fc96:0:535c"; };
                                                                            { "man2-5926-8e7-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b1a:10e:fc96:0:535c"; };
                                                                            { "man2-5927-958-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b1e:10e:fc96:0:535c"; };
                                                                            { "man2-5931-b2f-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:c17:10e:fc96:0:535c"; };
                                                                            { "man2-5932-a7f-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b25:10e:fc96:0:535c"; };
                                                                            { "man2-5945-be8-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:c0e:10e:fc96:0:535c"; };
                                                                            { "man2-5962-68f-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:813:10e:fc96:0:535c"; };
                                                                            { "man2-5987-d58-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:814:10e:fc96:0:535c"; };
                                                                            { "man2-5992-9f5-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:81a:10e:fc96:0:535c"; };
                                                                            { "man2-6006-cbc-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:c26:10e:fc96:0:535c"; };
                                                                            { "man2-6022-d10-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:c00:10e:fc96:0:535c"; };
                                                                            { "man2-6043-7d7-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b04:10e:fc96:0:535c"; };
                                                                            { "man2-6044-73c-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:c25:10e:fc96:0:535c"; };
                                                                            { "man2-6048-cc8-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:8a8:10e:fc96:0:535c"; };
                                                                            { "man2-6094-649-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:1885:10e:fc96:0:535c"; };
                                                                            { "man2-6142-57e-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:149b:10e:fc96:0:535c"; };
                                                                            { "man2-6146-40d-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:149d:10e:fc96:0:535c"; };
                                                                            { "man2-6163-2cc-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:14a5:10e:fc96:0:535c"; };
                                                                            { "man2-6165-954-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1499:10e:fc96:0:535c"; };
                                                                            { "man2-6216-f8d-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:14ac:10e:fc96:0:535c"; };
                                                                            { "man2-6231-b13-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1495:10e:fc96:0:535c"; };
                                                                            { "man2-6248-bfd-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1494:10e:fc96:0:535c"; };
                                                                            { "man2-6383-072-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:14a7:10e:fc96:0:535c"; };
                                                                            { "man2-6439-7ca-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1490:10e:fc96:0:535c"; };
                                                                            { "man2-6442-c8c-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:14ad:10e:fc96:0:535c"; };
                                                                            { "man2-6443-d73-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1496:10e:fc96:0:535c"; };
                                                                            { "man2-6459-403-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:149a:10e:fc96:0:535c"; };
                                                                            { "man2-6619-24b-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:27ad:10e:fc96:0:535c"; };
                                                                            { "man2-6620-ea2-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:279b:10e:fc96:0:535c"; };
                                                                            { "man2-6982-5f4-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:27a0:10e:fc96:0:535c"; };
                                                                            { "man2-6987-29e-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2793:10e:fc96:0:535c"; };
                                                                            { "man2-7032-433-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:27a1:10e:fc96:0:535c"; };
                                                                            { "man2-7068-802-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:279c:10e:fc96:0:535c"; };
                                                                            { "man2-7116-703-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:271d:10e:fc96:0:535c"; };
                                                                            { "man2-7389-6fc-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2786:10e:fc96:0:535c"; };
                                                                            { "man2-7400-7a6-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2798:10e:fc96:0:535c"; };
                                                                            { "man2-7401-643-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:279d:10e:fc96:0:535c"; };
                                                                            { "man2-7511-028-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:438e:10e:fc96:0:535c"; };
                                                                            { "man2-7513-ef1-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:43b3:10e:fc96:0:535c"; };
                                                                            { "man2-7514-b63-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:43b4:10e:fc96:0:535c"; };
                                                                            { "man2-7516-49d-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:438b:10e:fc96:0:535c"; };
                                                                            { "man2-7517-632-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:43b7:10e:fc96:0:535c"; };
                                                                            { "man2-7518-5cc-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4397:10e:fc96:0:535c"; };
                                                                            { "man2-7519-c5a-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4216:10e:fc96:0:535c"; };
                                                                            { "man2-7520-2f0-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:43be:10e:fc96:0:535c"; };
                                                                            { "man2-7522-186-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4217:10e:fc96:0:535c"; };
                                                                            { "man2-7523-724-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:438f:10e:fc96:0:535c"; };
                                                                            { "man2-7524-399-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:43b6:10e:fc96:0:535c"; };
                                                                            { "man2-7526-356-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4257:10e:fc96:0:535c"; };
                                                                            { "man2-7527-d06-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4396:10e:fc96:0:535c"; };
                                                                            { "man2-7529-ff1-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:43bf:10e:fc96:0:535c"; };
                                                                            { "man3-3113-844-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4c42:10e:fc96:0:535c"; };
                                                                            { "man3-3114-9cc-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4c23:10e:fc96:0:535c"; };
                                                                            { "man3-3115-051-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4214:10e:fc96:0:535c"; };
                                                                            { "man3-3116-c5d-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4236:10e:fc96:0:535c"; };
                                                                            { "man3-3117-ccc-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4234:10e:fc96:0:535c"; };
                                                                            { "man3-3118-f5c-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4084:10e:fc96:0:535c"; };
                                                                            { "man3-3120-510-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4c18:10e:fc96:0:535c"; };
                                                                            { "man3-3121-5e4-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4c39:10e:fc96:0:535c"; };
                                                                            { "man3-3122-7d9-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4237:10e:fc96:0:535c"; };
                                                                            { "man3-3123-1c7-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4398:10e:fc96:0:535c"; };
                                                                            { "man3-3124-500-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4c1a:10e:fc96:0:535c"; };
                                                                            { "man3-3125-c26-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4c40:10e:fc96:0:535c"; };
                                                                            { "man3-3126-675-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4c3b:10e:fc96:0:535c"; };
                                                                            { "man3-3127-43e-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:423f:10e:fc96:0:535c"; };
                                                                            { "man3-3128-799-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4c31:10e:fc96:0:535c"; };
                                                                            { "man3-3133-e1b-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4c0a:10e:fc96:0:535c"; };
                                                                            { "man3-3134-4d6-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4c19:10e:fc96:0:535c"; };
                                                                            { "man3-3135-199-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:43c0:10e:fc96:0:535c"; };
                                                                            { "man3-3136-29b-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4c3a:10e:fc96:0:535c"; };
                                                                            { "man3-3139-5aa-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:423c:10e:fc96:0:535c"; };
                                                                            { "man3-3140-fe5-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:423d:10e:fc96:0:535c"; };
                                                                            { "man3-3141-818-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4302:10e:fc96:0:535c"; };
                                                                            { "man3-3142-58c-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4235:10e:fc96:0:535c"; };
                                                                            { "man3-3146-076-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:420f:10e:fc96:0:535c"; };
                                                                            { "man3-3147-a0a-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4c11:10e:fc96:0:535c"; };
                                                                            { "man3-3148-7ab-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:430b:10e:fc96:0:535c"; };
                                                                            { "man3-3149-3f5-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4c4b:10e:fc96:0:535c"; };
                                                                            { "man3-3150-d5e-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:420e:10e:fc96:0:535c"; };
                                                                            { "man3-3173-e64-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4c12:10e:fc96:0:535c"; };
                                                                            { "man3-3175-a61-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4301:10e:fc96:0:535c"; };
                                                                            { "man3-3176-8ff-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:420d:10e:fc96:0:535c"; };
                                                                            { "man3-3177-284-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4c41:10e:fc96:0:535c"; };
                                                                            { "man3-3180-921-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4083:10e:fc96:0:535c"; };
                                                                            { "man3-3181-41e-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4c33:10e:fc96:0:535c"; };
                                                                            { "man3-3182-fa7-man-yabs-fronten-f92-21340.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:420c:10e:fc96:0:535c"; };
                                                                          }, {
                                                                            resolve_timeout = "10ms";
                                                                            connect_timeout = "50ms";
                                                                            backend_timeout = "10s";
                                                                            fail_on_5xx = true;
                                                                            http_backend = true;
                                                                            buffering = false;
                                                                            keepalive_count = 1;
                                                                            need_resolve = true;
                                                                            keepalive_timeout = "60s";
                                                                          }))
                                                                        }; -- rendezvous_hashing
                                                                      }; -- balancer2
                                                                    }; -- report
                                                                  }; -- bs_man56_meta
                                                                  bs_man80_meta = {
                                                                    weight = 1.000;
                                                                    report = {
                                                                      uuid = "bs_man80_meta";
                                                                      ranges = get_str_var("default_ranges");
                                                                      backend_time_ranges = "100ms,150ms,200ms,300ms,500ms";
                                                                      just_storage = false;
                                                                      disable_robotness = true;
                                                                      disable_sslness = true;
                                                                      events = {
                                                                        stats = "report";
                                                                      }; -- events
                                                                      balancer2 = {
                                                                        active_policy = {
                                                                          unique_policy = {};
                                                                        }; -- active_policy
                                                                        attempts = 1;
                                                                        rendezvous_hashing = {
                                                                          unpack(gen_proxy_backends({
                                                                            { "man1-0026-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:3673:10e:fc95:0:6d65"; };
                                                                            { "man1-0311-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:326c:10e:fc95:0:6d65"; };
                                                                            { "man1-0337-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:359f:10e:fc95:0:6d65"; };
                                                                            { "man1-0683-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:3605:10e:fc95:0:6d65"; };
                                                                            { "man1-0715-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:35f3:10e:fc95:0:6d65"; };
                                                                            { "man1-0726-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:3597:10e:fc95:0:6d65"; };
                                                                            { "man1-2766-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:257f:10e:fc95:0:6d65"; };
                                                                            { "man1-2885-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:322:10e:fc95:0:6d65"; };
                                                                            { "man1-2905-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:37a:10e:fc95:0:6d65"; };
                                                                            { "man1-2976-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:0:10e:fc95:0:6d65"; };
                                                                            { "man1-3297-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:3c22:10e:fc95:0:6d65"; };
                                                                            { "man1-3827-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:1ee5:10e:fc95:0:6d65"; };
                                                                            { "man1-3919-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:3722:10e:fc95:0:6d65"; };
                                                                            { "man1-4393-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:3aff:10e:fc95:0:6d65"; };
                                                                            { "man1-4437-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:36a6:10e:fc95:0:6d65"; };
                                                                            { "man1-4625-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:26ef:10e:fc95:0:6d65"; };
                                                                            { "man1-4802-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:2f0:10e:fc95:0:6d65"; };
                                                                            { "man1-6583-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:2f9:10e:fc95:0:6d65"; };
                                                                            { "man1-6777-bdc-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:5280:10e:fc95:0:6d65"; };
                                                                            { "man1-6782-0de-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:3e03:10e:fc95:0:6d65"; };
                                                                            { "man1-6783-7f6-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:3e02:10e:fc95:0:6d65"; };
                                                                            { "man1-6786-56f-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:4683:10e:fc95:0:6d65"; };
                                                                            { "man1-6797-9f2-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:3e89:10e:fc95:0:6d65"; };
                                                                            { "man1-6798-fe1-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:3e8b:10e:fc95:0:6d65"; };
                                                                            { "man1-6799-def-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:3e88:10e:fc95:0:6d65"; };
                                                                            { "man1-6800-826-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:3ea7:10e:fc95:0:6d65"; };
                                                                            { "man1-6804-e1e-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:432c:10e:fc95:0:6d65"; };
                                                                            { "man1-6805-520-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:432d:10e:fc95:0:6d65"; };
                                                                            { "man1-6810-331-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:3d26:10e:fc95:0:6d65"; };
                                                                            { "man1-6823-a53-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:4e0e:10e:fc95:0:6d65"; };
                                                                            { "man1-6826-794-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:4e0d:10e:fc95:0:6d65"; };
                                                                            { "man1-6828-25e-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:4e0c:10e:fc95:0:6d65"; };
                                                                            { "man1-6832-247-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:3da5:10e:fc95:0:6d65"; };
                                                                            { "man1-6835-838-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:3603:10e:fc95:0:6d65"; };
                                                                            { "man1-6836-12b-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:3602:10e:fc95:0:6d65"; };
                                                                            { "man1-7129-845-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:4e87:10e:fc95:0:6d65"; };
                                                                            { "man1-7130-7c0-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:4d8d:10e:fc95:0:6d65"; };
                                                                            { "man1-7134-a5a-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:5015:10e:fc95:0:6d65"; };
                                                                            { "man1-7135-9b0-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:5017:10e:fc95:0:6d65"; };
                                                                            { "man1-7136-5c1-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:5014:10e:fc95:0:6d65"; };
                                                                            { "man1-7137-c0d-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:5016:10e:fc95:0:6d65"; };
                                                                            { "man1-7432-9f2-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:4c86:10e:fc95:0:6d65"; };
                                                                            { "man1-7819-3ea-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:3587:10e:fc95:0:6d65"; };
                                                                            { "man1-8048-5dd-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:44ad:10e:fc95:0:6d65"; };
                                                                            { "man1-8049-e50-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:3327:10e:fc95:0:6d65"; };
                                                                            { "man1-8050-264-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:3325:10e:fc95:0:6d65"; };
                                                                            { "man1-8052-900-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:3324:10e:fc95:0:6d65"; };
                                                                            { "man1-8336-78d-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4215:10e:fc95:0:6d65"; };
                                                                            { "man1-8464-cb2-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:438c:10e:fc95:0:6d65"; };
                                                                            { "man1-9355-b83-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b80:10e:fc95:0:6d65"; };
                                                                            { "man1-9360-510-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b8d:10e:fc95:0:6d65"; };
                                                                            { "man1-9362-1ba-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:ba4:10e:fc95:0:6d65"; };
                                                                            { "man1-9496-978-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b81:10e:fc95:0:6d65"; };
                                                                            { "man1-9498-44c-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1509:10e:fc95:0:6d65"; };
                                                                            { "man1-9499-e70-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b8e:10e:fc95:0:6d65"; };
                                                                            { "man1-9510-471-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:80d:10e:fc95:0:6d65"; };
                                                                            { "man1-9585-6e6-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:f00:10e:fc95:0:6d65"; };
                                                                            { "man2-0477-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:5008:10e:fc95:0:6d65"; };
                                                                            { "man2-0507-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:506c:10e:fc95:0:6d65"; };
                                                                            { "man2-0508-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:5063:10e:fc95:0:6d65"; };
                                                                            { "man2-0512-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:5065:10e:fc95:0:6d65"; };
                                                                            { "man2-0514-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:5002:10e:fc95:0:6d65"; };
                                                                            { "man2-0523-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:5005:10e:fc95:0:6d65"; };
                                                                            { "man2-0527-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:5075:10e:fc95:0:6d65"; };
                                                                            { "man2-0535-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:5076:10e:fc95:0:6d65"; };
                                                                            { "man2-0682-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:5001:10e:fc95:0:6d65"; };
                                                                            { "man2-0686-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:505a:10e:fc95:0:6d65"; };
                                                                            { "man2-0689-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:501e:10e:fc95:0:6d65"; };
                                                                            { "man2-0695-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:5064:10e:fc95:0:6d65"; };
                                                                            { "man2-0699-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:5060:10e:fc95:0:6d65"; };
                                                                            { "man2-0703-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:506e:10e:fc95:0:6d65"; };
                                                                            { "man2-0707-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:5012:10e:fc95:0:6d65"; };
                                                                            { "man2-0708-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:5018:10e:fc95:0:6d65"; };
                                                                            { "man2-0716-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:506d:10e:fc95:0:6d65"; };
                                                                            { "man2-0717-36e-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:500c:10e:fc95:0:6d65"; };
                                                                            { "man2-0725-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:5007:10e:fc95:0:6d65"; };
                                                                            { "man2-0726-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:5026:10e:fc95:0:6d65"; };
                                                                            { "man2-0738-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:5020:10e:fc95:0:6d65"; };
                                                                            { "man2-0741-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:5011:10e:fc95:0:6d65"; };
                                                                            { "man2-0744-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:5019:10e:fc95:0:6d65"; };
                                                                            { "man2-0753-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:5077:10e:fc95:0:6d65"; };
                                                                            { "man2-0762-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:5021:10e:fc95:0:6d65"; };
                                                                            { "man2-0763-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:500f:10e:fc95:0:6d65"; };
                                                                            { "man2-0764-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:5068:10e:fc95:0:6d65"; };
                                                                            { "man2-0766-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:505b:10e:fc95:0:6d65"; };
                                                                            { "man2-0767-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:5009:10e:fc95:0:6d65"; };
                                                                            { "man2-0771-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:5067:10e:fc95:0:6d65"; };
                                                                            { "man2-0772-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:500b:10e:fc95:0:6d65"; };
                                                                            { "man2-0775-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4d03:10e:fc95:0:6d65"; };
                                                                            { "man2-0781-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4d2d:10e:fc95:0:6d65"; };
                                                                            { "man2-0782-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4d25:10e:fc95:0:6d65"; };
                                                                            { "man2-0800-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4d17:10e:fc95:0:6d65"; };
                                                                            { "man2-0832-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4d23:10e:fc95:0:6d65"; };
                                                                            { "man2-0834-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4d21:10e:fc95:0:6d65"; };
                                                                            { "man2-0845-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4d27:10e:fc95:0:6d65"; };
                                                                            { "man2-0849-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4d1d:10e:fc95:0:6d65"; };
                                                                            { "man2-0850-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4d1f:10e:fc95:0:6d65"; };
                                                                            { "man2-0857-man-yabs-frontend-serve-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:4d01:10e:fc95:0:6d65"; };
                                                                            { "man2-2671-f19-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b90:10e:fc95:0:6d65"; };
                                                                            { "man2-2672-523-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b9f:10e:fc95:0:6d65"; };
                                                                            { "man2-2673-f29-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b88:10e:fc95:0:6d65"; };
                                                                            { "man2-2674-67e-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:918:10e:fc95:0:6d65"; };
                                                                            { "man2-2675-6fc-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:90f:10e:fc95:0:6d65"; };
                                                                            { "man2-2676-477-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:901:10e:fc95:0:6d65"; };
                                                                            { "man2-2677-106-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:91a:10e:fc95:0:6d65"; };
                                                                            { "man2-2678-ebd-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:220b:10e:fc95:0:6d65"; };
                                                                            { "man2-2680-c1a-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2223:10e:fc95:0:6d65"; };
                                                                            { "man2-2681-87e-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2222:10e:fc95:0:6d65"; };
                                                                            { "man2-2682-ada-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:90e:10e:fc95:0:6d65"; };
                                                                            { "man2-2683-c67-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:50c:10e:fc95:0:6d65"; };
                                                                            { "man2-2684-450-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:924:10e:fc95:0:6d65"; };
                                                                            { "man2-2685-637-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:51f:10e:fc95:0:6d65"; };
                                                                            { "man2-2686-425-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:11a4:10e:fc95:0:6d65"; };
                                                                            { "man2-2687-63b-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2212:10e:fc95:0:6d65"; };
                                                                            { "man2-2688-2ab-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:504:10e:fc95:0:6d65"; };
                                                                            { "man2-2689-70b-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:118f:10e:fc95:0:6d65"; };
                                                                            { "man2-2690-0b0-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:159a:10e:fc95:0:6d65"; };
                                                                            { "man2-2691-044-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:52d:10e:fc95:0:6d65"; };
                                                                            { "man2-2692-79e-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1194:10e:fc95:0:6d65"; };
                                                                            { "man2-2693-bc3-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2209:10e:fc95:0:6d65"; };
                                                                            { "man2-2694-fa3-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:118e:10e:fc95:0:6d65"; };
                                                                            { "man2-2695-c4a-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:900:10e:fc95:0:6d65"; };
                                                                            { "man2-2696-bec-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1597:10e:fc95:0:6d65"; };
                                                                            { "man2-2697-c8a-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:159e:10e:fc95:0:6d65"; };
                                                                            { "man2-2698-391-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1589:10e:fc95:0:6d65"; };
                                                                            { "man2-2699-bef-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:158c:10e:fc95:0:6d65"; };
                                                                            { "man2-2700-e6f-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1582:10e:fc95:0:6d65"; };
                                                                            { "man2-2701-86c-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1189:10e:fc95:0:6d65"; };
                                                                            { "man2-2702-b95-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1500:10e:fc95:0:6d65"; };
                                                                            { "man2-2703-7e9-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1590:10e:fc95:0:6d65"; };
                                                                            { "man2-2704-4b2-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1181:10e:fc95:0:6d65"; };
                                                                            { "man2-2705-ee3-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:151e:10e:fc95:0:6d65"; };
                                                                            { "man2-2706-e92-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1508:10e:fc95:0:6d65"; };
                                                                            { "man2-2707-499-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1526:10e:fc95:0:6d65"; };
                                                                            { "man2-2708-d5b-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:151a:10e:fc95:0:6d65"; };
                                                                            { "man2-2709-85c-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:151f:10e:fc95:0:6d65"; };
                                                                            { "man2-2710-9f4-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:11ad:10e:fc95:0:6d65"; };
                                                                            { "man2-2711-d2b-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:11a5:10e:fc95:0:6d65"; };
                                                                            { "man2-2712-ca0-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b9c:10e:fc95:0:6d65"; };
                                                                            { "man2-2713-87c-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b86:10e:fc95:0:6d65"; };
                                                                            { "man2-2714-a47-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:92c:10e:fc95:0:6d65"; };
                                                                            { "man2-2715-3ff-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b94:10e:fc95:0:6d65"; };
                                                                            { "man2-2716-60e-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:ba3:10e:fc95:0:6d65"; };
                                                                            { "man2-2717-8ff-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1199:10e:fc95:0:6d65"; };
                                                                            { "man2-2718-a37-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b85:10e:fc95:0:6d65"; };
                                                                            { "man2-2719-aa9-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:ba7:10e:fc95:0:6d65"; };
                                                                            { "man2-2720-69a-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:ba8:10e:fc95:0:6d65"; };
                                                                            { "man2-2721-1b7-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:927:10e:fc95:0:6d65"; };
                                                                            { "man2-2722-9d8-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b92:10e:fc95:0:6d65"; };
                                                                            { "man2-2723-f68-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b82:10e:fc95:0:6d65"; };
                                                                            { "man2-2724-3e8-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:915:10e:fc95:0:6d65"; };
                                                                            { "man2-2725-7cc-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2200:10e:fc95:0:6d65"; };
                                                                            { "man2-2726-670-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:ba5:10e:fc95:0:6d65"; };
                                                                            { "man2-2727-0aa-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b9b:10e:fc95:0:6d65"; };
                                                                            { "man2-2731-df5-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b91:10e:fc95:0:6d65"; };
                                                                            { "man2-2754-9c2-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b8b:10e:fc95:0:6d65"; };
                                                                            { "man2-2771-09d-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1185:10e:fc95:0:6d65"; };
                                                                            { "man2-2773-32c-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1598:10e:fc95:0:6d65"; };
                                                                            { "man2-2775-914-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1186:10e:fc95:0:6d65"; };
                                                                            { "man2-2777-1a3-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:118d:10e:fc95:0:6d65"; };
                                                                            { "man2-2780-153-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:119c:10e:fc95:0:6d65"; };
                                                                            { "man2-2784-b8e-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1196:10e:fc95:0:6d65"; };
                                                                            { "man2-2786-349-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1197:10e:fc95:0:6d65"; };
                                                                            { "man2-2787-66c-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1594:10e:fc95:0:6d65"; };
                                                                            { "man2-2789-6a9-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1192:10e:fc95:0:6d65"; };
                                                                            { "man2-2790-02e-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1592:10e:fc95:0:6d65"; };
                                                                            { "man2-2791-061-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1183:10e:fc95:0:6d65"; };
                                                                            { "man2-2792-4f9-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:15ad:10e:fc95:0:6d65"; };
                                                                            { "man2-2793-9a2-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:15a4:10e:fc95:0:6d65"; };
                                                                            { "man2-2794-b41-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:15a3:10e:fc95:0:6d65"; };
                                                                            { "man2-2795-a04-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:118b:10e:fc95:0:6d65"; };
                                                                            { "man2-2796-1c0-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1581:10e:fc95:0:6d65"; };
                                                                            { "man2-2797-a1a-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:15a0:10e:fc95:0:6d65"; };
                                                                            { "man2-2801-bc0-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:118a:10e:fc95:0:6d65"; };
                                                                            { "man2-2802-079-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:158b:10e:fc95:0:6d65"; };
                                                                            { "man2-2803-8b3-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:15a6:10e:fc95:0:6d65"; };
                                                                            { "man2-2804-135-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:159c:10e:fc95:0:6d65"; };
                                                                            { "man2-2805-9e5-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1599:10e:fc95:0:6d65"; };
                                                                            { "man2-2806-0f7-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1583:10e:fc95:0:6d65"; };
                                                                            { "man2-2807-c06-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:159d:10e:fc95:0:6d65"; };
                                                                            { "man2-2808-9f4-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:119e:10e:fc95:0:6d65"; };
                                                                            { "man2-2811-dc4-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1584:10e:fc95:0:6d65"; };
                                                                            { "man2-2812-04d-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:158f:10e:fc95:0:6d65"; };
                                                                            { "man2-2813-a84-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:158e:10e:fc95:0:6d65"; };
                                                                            { "man2-2814-bd1-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1190:10e:fc95:0:6d65"; };
                                                                            { "man2-2817-f75-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1593:10e:fc95:0:6d65"; };
                                                                            { "man2-2821-59f-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:11a0:10e:fc95:0:6d65"; };
                                                                            { "man2-2823-306-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1198:10e:fc95:0:6d65"; };
                                                                            { "man2-2824-491-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:11a7:10e:fc95:0:6d65"; };
                                                                            { "man2-2825-427-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:11a6:10e:fc95:0:6d65"; };
                                                                            { "man2-2827-120-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:158a:10e:fc95:0:6d65"; };
                                                                            { "man2-2828-513-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1182:10e:fc95:0:6d65"; };
                                                                            { "man2-2831-952-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:15a1:10e:fc95:0:6d65"; };
                                                                            { "man2-2832-f4f-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:15a5:10e:fc95:0:6d65"; };
                                                                            { "man2-2833-be0-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:118c:10e:fc95:0:6d65"; };
                                                                            { "man2-2840-cf5-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:119b:10e:fc95:0:6d65"; };
                                                                            { "man2-2845-afa-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1595:10e:fc95:0:6d65"; };
                                                                            { "man2-2846-4db-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1188:10e:fc95:0:6d65"; };
                                                                            { "man2-2848-922-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1580:10e:fc95:0:6d65"; };
                                                                            { "man2-2851-b26-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:15a2:10e:fc95:0:6d65"; };
                                                                            { "man2-2852-bfe-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:11a2:10e:fc95:0:6d65"; };
                                                                            { "man2-2854-9a2-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1191:10e:fc95:0:6d65"; };
                                                                            { "man2-2856-1e3-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:11a3:10e:fc95:0:6d65"; };
                                                                            { "man2-2858-f91-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:159f:10e:fc95:0:6d65"; };
                                                                            { "man2-2859-470-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1596:10e:fc95:0:6d65"; };
                                                                            { "man2-2862-aaa-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1585:10e:fc95:0:6d65"; };
                                                                            { "man2-2863-3aa-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1195:10e:fc95:0:6d65"; };
                                                                            { "man2-2864-d8a-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:159b:10e:fc95:0:6d65"; };
                                                                            { "man2-2870-18b-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:15a7:10e:fc95:0:6d65"; };
                                                                            { "man2-2871-8d6-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1588:10e:fc95:0:6d65"; };
                                                                            { "man2-2876-4ea-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1591:10e:fc95:0:6d65"; };
                                                                            { "man2-2880-4df-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1587:10e:fc95:0:6d65"; };
                                                                            { "man2-2886-65f-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1180:10e:fc95:0:6d65"; };
                                                                            { "man2-2887-6cf-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:b93:10e:fc95:0:6d65"; };
                                                                            { "man2-6123-399-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2a16:10e:fc95:0:6d65"; };
                                                                            { "man2-6337-50e-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2627:10e:fc95:0:6d65"; };
                                                                            { "man2-6493-ba3-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:291c:10e:fc95:0:6d65"; };
                                                                            { "man2-6566-9a4-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2417:10e:fc95:0:6d65"; };
                                                                            { "man2-6599-aff-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2d91:10e:fc95:0:6d65"; };
                                                                            { "man2-6695-ef6-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:f84:10e:fc95:0:6d65"; };
                                                                            { "man2-6761-b99-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:1f15:10e:fc95:0:6d65"; };
                                                                            { "man2-6919-771-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2415:10e:fc95:0:6d65"; };
                                                                            { "man2-6967-a85-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c13:1e9f:10e:fc95:0:6d65"; };
                                                                            { "man2-7037-0ca-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2903:10e:fc95:0:6d65"; };
                                                                            { "man2-7084-97d-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:fa6:10e:fc95:0:6d65"; };
                                                                            { "man2-7318-5b9-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:2410:10e:fc95:0:6d65"; };
                                                                            { "man2-7405-829-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0a:25a5:10e:fc95:0:6d65"; };
                                                                            { "man2-7432-342-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:40e:10e:fc95:0:6d65"; };
                                                                            { "man3-5176-3e8-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:40a:10e:fc95:0:6d65"; };
                                                                            { "man3-5178-f52-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:400:10e:fc95:0:6d65"; };
                                                                            { "man3-5181-4c7-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:404:10e:fc95:0:6d65"; };
                                                                            { "man3-5189-3d0-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:474:10e:fc95:0:6d65"; };
                                                                            { "man3-5211-7a6-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:406:10e:fc95:0:6d65"; };
                                                                            { "man3-5215-d05-man-yabs-fronten-7a5-28005.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c0b:414:10e:fc95:0:6d65"; };
                                                                          }, {
                                                                            resolve_timeout = "10ms";
                                                                            connect_timeout = "50ms";
                                                                            backend_timeout = "10s";
                                                                            fail_on_5xx = true;
                                                                            http_backend = true;
                                                                            buffering = false;
                                                                            keepalive_count = 1;
                                                                            need_resolve = true;
                                                                            keepalive_timeout = "60s";
                                                                          }))
                                                                        }; -- rendezvous_hashing
                                                                      }; -- balancer2
                                                                    }; -- report
                                                                  }; -- bs_man80_meta
                                                                  bs_myt32_meta = {
                                                                    weight = 1.000;
                                                                    report = {
                                                                      uuid = "bs_myt32_meta";
                                                                      ranges = get_str_var("default_ranges");
                                                                      backend_time_ranges = "100ms,150ms,200ms,300ms,500ms";
                                                                      just_storage = false;
                                                                      disable_robotness = true;
                                                                      disable_sslness = true;
                                                                      events = {
                                                                        stats = "report";
                                                                      }; -- events
                                                                      balancer2 = {
                                                                        active_policy = {
                                                                          unique_policy = {};
                                                                        }; -- active_policy
                                                                        attempts = 1;
                                                                        rendezvous_hashing = {
                                                                          unpack(gen_proxy_backends({
                                                                            { "myt1-2629-6f4-msk-myt-yabs-fro-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c12:d00:10e:fc91:0:5bbf"; };
                                                                            { "myt1-4655-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1f1e:10e:fc91:0:5bbf"; };
                                                                            { "myt1-4658-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1f8d:10e:fc91:0:5bbf"; };
                                                                            { "myt1-4675-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1039:10e:fc91:0:5bbf"; };
                                                                            { "myt1-4691-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:8b7:10e:fc91:0:5bbf"; };
                                                                            { "myt1-4693-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:8ac:10e:fc91:0:5bbf"; };
                                                                            { "myt1-4697-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1f3b:10e:fc91:0:5bbf"; };
                                                                            { "myt1-4704-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1120:10e:fc91:0:5bbf"; };
                                                                            { "myt1-4706-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1eb8:10e:fc91:0:5bbf"; };
                                                                            { "myt1-4707-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:936:10e:fc91:0:5bbf"; };
                                                                            { "myt1-4709-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:5a5:10e:fc91:0:5bbf"; };
                                                                            { "myt1-4714-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:103b:10e:fc91:0:5bbf"; };
                                                                            { "myt1-4733-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1eb3:10e:fc91:0:5bbf"; };
                                                                            { "myt1-4736-5ed-msk-myt-yabs-fro-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:213c:10e:fc91:0:5bbf"; };
                                                                            { "myt1-4738-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:5c6:10e:fc91:0:5bbf"; };
                                                                            { "myt1-4741-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1f9a:10e:fc91:0:5bbf"; };
                                                                            { "myt1-4991-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:24a2:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5039-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:2482:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5051-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:248a:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5064-795-msk-myt-yabs-fro-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:128a:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5065-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:24ac:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5156-8e6-msk-myt-yabs-fro-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:248e:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5233-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:2489:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5253-efd-msk-myt-yabs-fro-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1034:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5296-042-msk-myt-yabs-fro-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:2496:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5561-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:103a:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5562-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:c1a:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5564-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:f3a:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5566-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1eb0:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5568-3c1-msk-myt-yabs-fro-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:d38:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5575-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1132:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5576-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1f34:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5581-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1f20:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5590-f1e-msk-myt-yabs-fro-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:943:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5594-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1fbc:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5600-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:5c5:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5602-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:7bb:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5603-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:101a:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5613-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:c31:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5616-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:f44:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5620-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1019:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5624-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:c32:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5625-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1121:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5627-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1124:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5634-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1130:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5637-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:f17:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5639-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1143:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5643-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:5a4:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5649-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:c44:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5653-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:7b1:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5656-c74-msk-myt-yabs-fro-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:213d:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5659-760-msk-myt-yabs-fro-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:d1b:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5664-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:7c2:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5665-f6a-msk-myt-yabs-fro-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:2123:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5675-f2f-msk-myt-yabs-fro-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:d18:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5684-748-msk-myt-yabs-fro-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:d41:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5687-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1e91:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5688-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:c19:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5691-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:7ce:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5695-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:c3b:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5697-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1eb6:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5699-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:f36:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5713-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:8a2:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5715-fb2-msk-myt-yabs-fro-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:2010:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5717-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:c41:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5721-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:93c:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5735-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1f10:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5736-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:8b1:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5738-555-msk-myt-yabs-fro-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:c4e:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5740-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1131:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5743-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:a89:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5758-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1ea6:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5761-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:925:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5765-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1f9c:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5767-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:58d:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5772-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1eb5:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5780-e20-msk-myt-yabs-fro-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:d2a:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5781-1b3-msk-myt-yabs-fro-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:cbc:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5782-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:8b9:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5784-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1f3f:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5789-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:7b7:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5794-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:5c1:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5797-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1fa8:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5800-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1f31:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5801-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:59b:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5803-ce2-msk-myt-yabs-fro-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:d39:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5804-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1f28:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5806-333-msk-myt-yabs-fro-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:2018:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5811-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1fac:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5814-873-msk-myt-yabs-fro-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:c14:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5818-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1110:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5820-f8b-msk-myt-yabs-fro-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:d10:10e:fc91:0:5bbf"; };
                                                                            { "myt1-5823-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1f26:10e:fc91:0:5bbf"; };
                                                                            { "myt1-6185-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1fb2:10e:fc91:0:5bbf"; };
                                                                            { "myt1-6186-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:7c0:10e:fc91:0:5bbf"; };
                                                                            { "myt1-6187-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:7c1:10e:fc91:0:5bbf"; };
                                                                            { "myt1-6188-619-msk-myt-yabs-fro-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:d23:10e:fc91:0:5bbf"; };
                                                                            { "myt1-6191-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:945:10e:fc91:0:5bbf"; };
                                                                            { "myt1-6196-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:5b8:10e:fc91:0:5bbf"; };
                                                                            { "myt1-6198-b8b-msk-myt-yabs-fro-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:d30:10e:fc91:0:5bbf"; };
                                                                            { "myt1-6201-360-msk-myt-yabs-fro-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:2111:10e:fc91:0:5bbf"; };
                                                                            { "myt1-6202-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:93d:10e:fc91:0:5bbf"; };
                                                                            { "myt1-6211-33b-msk-myt-yabs-fro-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:2131:10e:fc91:0:5bbf"; };
                                                                            { "myt1-6212-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:89b:10e:fc91:0:5bbf"; };
                                                                            { "myt1-6225-msk-myt-yabs-frontend-s-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1ea3:10e:fc91:0:5bbf"; };
                                                                            { "myt1-6233-67e-msk-myt-yabs-fro-6f2-23487.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:ca4:10e:fc91:0:5bbf"; };
                                                                          }, {
                                                                            resolve_timeout = "10ms";
                                                                            connect_timeout = "50ms";
                                                                            backend_timeout = "10s";
                                                                            fail_on_5xx = true;
                                                                            http_backend = true;
                                                                            buffering = false;
                                                                            keepalive_count = 1;
                                                                            need_resolve = true;
                                                                            keepalive_timeout = "60s";
                                                                          }))
                                                                        }; -- rendezvous_hashing
                                                                      }; -- balancer2
                                                                    }; -- report
                                                                  }; -- bs_myt32_meta
                                                                  bs_myt56_meta = {
                                                                    weight = 1.000;
                                                                    report = {
                                                                      uuid = "bs_myt56_meta";
                                                                      ranges = get_str_var("default_ranges");
                                                                      backend_time_ranges = "100ms,150ms,200ms,300ms,500ms";
                                                                      just_storage = false;
                                                                      disable_robotness = true;
                                                                      disable_sslness = true;
                                                                      events = {
                                                                        stats = "report";
                                                                      }; -- events
                                                                      balancer2 = {
                                                                        active_policy = {
                                                                          unique_policy = {};
                                                                        }; -- active_policy
                                                                        attempts = 1;
                                                                        rendezvous_hashing = {
                                                                          unpack(gen_proxy_backends({
                                                                            { "myt1-0172-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:694:10e:fc8f:0:3409"; };
                                                                            { "myt1-0173-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:6da:10e:fc8f:0:3409"; };
                                                                            { "myt1-0175-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:6c2:10e:fc8f:0:3409"; };
                                                                            { "myt1-0240-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:a8f:10e:fc8f:0:3409"; };
                                                                            { "myt1-0246-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1b8b:10e:fc8f:0:3409"; };
                                                                            { "myt1-0264-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1bfd:10e:fc8f:0:3409"; };
                                                                            { "myt1-0265-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1bff:10e:fc8f:0:3409"; };
                                                                            { "myt1-0268-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1ba9:10e:fc8f:0:3409"; };
                                                                            { "myt1-0272-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:9c9:10e:fc8f:0:3409"; };
                                                                            { "myt1-0276-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:ac2:10e:fc8f:0:3409"; };
                                                                            { "myt1-0277-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:add:10e:fc8f:0:3409"; };
                                                                            { "myt1-0278-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:a87:10e:fc8f:0:3409"; };
                                                                            { "myt1-0352-203-msk-myt-yabs-fro-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:9c3:10e:fc8f:0:3409"; };
                                                                            { "myt1-0353-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:9d6:10e:fc8f:0:3409"; };
                                                                            { "myt1-0354-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:995:10e:fc8f:0:3409"; };
                                                                            { "myt1-0415-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1d4c:10e:fc8f:0:3409"; };
                                                                            { "myt1-0433-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1d4e:10e:fc8f:0:3409"; };
                                                                            { "myt1-0434-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1d16:10e:fc8f:0:3409"; };
                                                                            { "myt1-0471-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:6b9:10e:fc8f:0:3409"; };
                                                                            { "myt1-0538-1db-msk-myt-yabs-fro-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:2a2:10e:fc8f:0:3409"; };
                                                                            { "myt1-0539-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:2eb:10e:fc8f:0:3409"; };
                                                                            { "myt1-0540-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:292:10e:fc8f:0:3409"; };
                                                                            { "myt1-0542-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:290:10e:fc8f:0:3409"; };
                                                                            { "myt1-0545-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1d58:10e:fc8f:0:3409"; };
                                                                            { "myt1-0588-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:471:10e:fc8f:0:3409"; };
                                                                            { "myt1-0591-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:405:10e:fc8f:0:3409"; };
                                                                            { "myt1-0592-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:473:10e:fc8f:0:3409"; };
                                                                            { "myt1-0593-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:422:10e:fc8f:0:3409"; };
                                                                            { "myt1-0808-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1183:10e:fc8f:0:3409"; };
                                                                            { "myt1-0816-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1189:10e:fc8f:0:3409"; };
                                                                            { "myt1-0834-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1187:10e:fc8f:0:3409"; };
                                                                            { "myt1-0840-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:1184:10e:fc8f:0:3409"; };
                                                                            { "myt1-1623-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:708:10e:fc8f:0:3409"; };
                                                                            { "myt1-1624-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:709:10e:fc8f:0:3409"; };
                                                                            { "myt1-1625-2af-msk-myt-yabs-fro-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:711:10e:fc8f:0:3409"; };
                                                                            { "myt1-1626-msk-myt-yabs-frontend-s-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c00:70b:10e:fc8f:0:3409"; };
                                                                            { "myt1-2617-d00-msk-myt-yabs-fro-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c12:e80:10e:fc8f:0:3409"; };
                                                                            { "myt1-2620-fe0-msk-myt-yabs-fro-305-13321.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c12:d8c:10e:fc8f:0:3409"; };
                                                                          }, {
                                                                            resolve_timeout = "10ms";
                                                                            connect_timeout = "50ms";
                                                                            backend_timeout = "10s";
                                                                            fail_on_5xx = true;
                                                                            http_backend = true;
                                                                            buffering = false;
                                                                            keepalive_count = 1;
                                                                            need_resolve = true;
                                                                            keepalive_timeout = "60s";
                                                                          }))
                                                                        }; -- rendezvous_hashing
                                                                      }; -- balancer2
                                                                    }; -- report
                                                                  }; -- bs_myt56_meta
                                                                  bs_myt_infra1 = {
                                                                    weight = 1.000;
                                                                    report = {
                                                                      uuid = "bs_myt_infra1";
                                                                      ranges = get_str_var("default_ranges");
                                                                      backend_time_ranges = "100ms,150ms,200ms,300ms,500ms";
                                                                      just_storage = false;
                                                                      disable_robotness = true;
                                                                      disable_sslness = true;
                                                                      events = {
                                                                        stats = "report";
                                                                      }; -- events
                                                                      balancer2 = {
                                                                        active_policy = {
                                                                          unique_policy = {};
                                                                        }; -- active_policy
                                                                        attempts = 1;
                                                                        rendezvous_hashing = {
                                                                          unpack(gen_proxy_backends({
                                                                            { "myt1-2269-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:2084:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2270-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:2086:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2271-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:2085:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2315-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:208d:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2318-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:208f:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2319-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:2094:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2320-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:2087:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2321-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:2080:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2322-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:208c:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2336-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:2095:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2342-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:2097:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2343-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:2082:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2346-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:20a0:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2348-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:208e:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2359-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:20a4:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2368-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:20a5:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2371-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:2096:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2376-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:20a7:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2377-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:209d:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2402-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:209f:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2405-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:20a3:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2414-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:209c:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2417-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:2093:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2430-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:20a2:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2445-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:209b:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2464-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:209a:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2465-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:2098:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2466-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:2099:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2474-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:2083:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2478-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:2088:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2479-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:20a6:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2481-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:2092:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2482-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:2091:10d:ae0b:0:3d09"; };
                                                                            { "myt1-2483-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:2090:10d:ae0b:0:3d09"; };
                                                                            { "myt1-3397-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:208b:10d:ae0b:0:3d09"; };
                                                                            { "myt1-3402-msk-myt-yabs-frontend-s-799-15625.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c00:208a:10d:ae0b:0:3d09"; };
                                                                          }, {
                                                                            resolve_timeout = "10ms";
                                                                            connect_timeout = "50ms";
                                                                            backend_timeout = "10s";
                                                                            fail_on_5xx = true;
                                                                            http_backend = true;
                                                                            buffering = false;
                                                                            keepalive_count = 1;
                                                                            need_resolve = true;
                                                                            keepalive_timeout = "60s";
                                                                          }))
                                                                        }; -- rendezvous_hashing
                                                                      }; -- balancer2
                                                                    }; -- report
                                                                  }; -- bs_myt_infra1
                                                                  bs_sas32_meta = {
                                                                    weight = 1.000;
                                                                    report = {
                                                                      uuid = "bs_sas32_meta";
                                                                      ranges = get_str_var("default_ranges");
                                                                      backend_time_ranges = "100ms,150ms,200ms,300ms,500ms";
                                                                      just_storage = false;
                                                                      disable_robotness = true;
                                                                      disable_sslness = true;
                                                                      events = {
                                                                        stats = "report";
                                                                      }; -- events
                                                                      balancer2 = {
                                                                        active_policy = {
                                                                          unique_policy = {};
                                                                        }; -- active_policy
                                                                        attempts = 1;
                                                                        rendezvous_hashing = {
                                                                          unpack(gen_proxy_backends({
                                                                            { "sas0-8415-a07-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2234:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8416-22e-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d20:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8417-30b-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:222f:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8427-f51-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d4d:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8428-177-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d2d:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8429-0f0-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d31:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8438-46f-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d22:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8440-a58-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d48:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8441-4cc-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2247:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8442-8f0-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2223:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8444-119-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d3f:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8446-a07-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d45:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8447-bcc-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d23:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8449-a68-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d3a:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8450-34c-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:224c:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8452-93b-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:223e:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8456-b4e-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2225:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8466-aad-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:224b:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8469-cc1-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d4f:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8470-ee3-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2243:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8471-767-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d3c:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8472-e6e-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d36:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8477-173-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d41:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8480-f77-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2242:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8481-bc5-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2233:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8482-3e0-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d37:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8484-ff1-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2221:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8485-4d8-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d24:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8486-eec-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2222:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8487-00a-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2244:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8490-5e8-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2239:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8494-fc5-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d21:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8497-c3a-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:222b:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8498-a03-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d26:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8503-4e7-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2229:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8505-51f-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d43:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8509-6f6-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d3d:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8510-2e3-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2224:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8512-eb3-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2240:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8514-9b3-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d42:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8515-96c-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d38:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8517-9b4-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2248:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8518-fcd-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:223f:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8519-1ce-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:223c:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8522-ece-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2227:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8526-f26-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d49:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8530-37f-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:223a:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8531-952-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d40:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8540-950-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d39:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8544-def-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:224a:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8557-316-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2238:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8560-a42-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d34:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8562-8df-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2148:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8567-4e0-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2147:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8570-bad-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d32:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8574-2ad-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2245:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8576-b93-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d27:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8583-bbf-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:212f:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8589-12c-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2231:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8590-a61-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:213c:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8591-e27-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d46:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8592-294-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d28:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8594-a46-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d2b:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8596-4a3-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2144:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8600-672-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2241:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8601-911-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2226:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8605-ad3-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d47:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8606-478-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d3e:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8608-bec-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:223b:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8609-c95-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d4b:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8610-a08-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2132:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8611-b0e-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2230:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8612-23f-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d29:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8614-c90-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:212a:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8615-258-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d44:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8619-fb9-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2141:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8621-91a-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d25:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8624-399-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d4c:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8625-4cc-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2134:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8629-571-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:214e:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8632-0a2-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d33:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8637-6a8-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2235:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8638-5db-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2149:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8639-b25-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2146:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8640-da6-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2228:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8642-65c-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:214b:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8645-979-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1d2f:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8646-5f9-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2220:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8647-c8a-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2246:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8649-c94-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2237:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8651-974-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2236:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8652-137-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:224d:10e:fcc6:0:41ac"; };
                                                                            { "sas0-8655-809-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:222d:10e:fcc6:0:41ac"; };
                                                                            { "sas2-4986-868-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3114:10e:fcc6:0:41ac"; };
                                                                            { "sas2-4988-d4a-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3137:10e:fcc6:0:41ac"; };
                                                                            { "sas2-4990-f88-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:312b:10e:fcc6:0:41ac"; };
                                                                            { "sas2-4997-dc2-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:312e:10e:fcc6:0:41ac"; };
                                                                            { "sas2-4999-29a-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3117:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5002-e18-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3108:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5036-6a9-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:313d:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5040-173-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:310a:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5042-e02-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3115:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5045-0e3-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3134:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5048-582-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3121:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5049-b50-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3136:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5050-d09-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3120:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5055-b2a-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:312a:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5056-eb9-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3122:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5058-f69-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3103:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5061-4e1-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3131:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5064-ed5-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3135:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5066-882-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3109:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5087-bd1-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3144:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5089-edd-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3107:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5090-79e-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:310b:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5091-d7d-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3129:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5095-443-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3101:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5099-4bb-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3105:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5101-560-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3116:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5102-732-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3145:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5105-3f0-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3128:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5107-0a2-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:313c:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5108-6a0-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3104:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5117-853-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3119:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5121-f95-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:313e:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5125-311-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:310d:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5259-f5d-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3102:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5266-007-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:313b:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5267-2b4-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3100:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5268-fef-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3106:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5270-cc6-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:310f:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5273-abd-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:311c:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5274-16a-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:310e:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5278-3fb-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:312f:10e:fcc6:0:41ac"; };
                                                                            { "sas2-5302-c6a-sas-yabs-fronten-007-16812.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:310c:10e:fcc6:0:41ac"; };
                                                                          }, {
                                                                            resolve_timeout = "10ms";
                                                                            connect_timeout = "50ms";
                                                                            backend_timeout = "10s";
                                                                            fail_on_5xx = true;
                                                                            http_backend = true;
                                                                            buffering = false;
                                                                            keepalive_count = 1;
                                                                            need_resolve = true;
                                                                            keepalive_timeout = "60s";
                                                                          }))
                                                                        }; -- rendezvous_hashing
                                                                      }; -- balancer2
                                                                    }; -- report
                                                                  }; -- bs_sas32_meta
                                                                  bs_sas56_meta = {
                                                                    weight = 1.000;
                                                                    report = {
                                                                      uuid = "bs_sas56_meta";
                                                                      ranges = get_str_var("default_ranges");
                                                                      backend_time_ranges = "100ms,150ms,200ms,300ms,500ms";
                                                                      just_storage = false;
                                                                      disable_robotness = true;
                                                                      disable_sslness = true;
                                                                      events = {
                                                                        stats = "report";
                                                                      }; -- events
                                                                      balancer2 = {
                                                                        active_policy = {
                                                                          unique_policy = {};
                                                                        }; -- active_policy
                                                                        attempts = 1;
                                                                        rendezvous_hashing = {
                                                                          unpack(gen_proxy_backends({
                                                                            { "sas0-0177-7cf-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c08:4c02:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-0184-cfc-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c08:6683:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-0189-dd0-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c08:d905:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-0225-352-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c08:4c03:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-0232-301-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c08:5f82:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-0260-104-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c08:d906:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-0322-2c2-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c07:1003:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-0329-f20-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c08:d907:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-0340-562-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c08:d904:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-5626-007-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:588:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-5628-6aa-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:590:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-5629-5a0-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:58e:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-5630-dfe-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:58c:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-5631-17b-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:5a3:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-5632-ece-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:59c:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-5633-c6a-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:5a4:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-5634-78b-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:596:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-5635-156-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:58a:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-5636-5a2-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:585:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-5637-8ec-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:5a0:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-6114-d81-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:5a1:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-6123-0bc-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:598:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-6126-6cd-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:580:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-6127-084-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:59f:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-8023-58a-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:f21:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-8024-ca4-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:f28:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-8025-c0c-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:f25:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-8032-b2d-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:33ca:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-8041-d1a-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:f2b:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-8042-72e-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:f2c:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-8060-f02-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:33b3:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-8091-0ca-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:33c1:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-8111-05c-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:f32:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-8113-200-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:f2a:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-8116-2ca-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:f27:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-8118-896-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:f33:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-8119-b07-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:f31:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-8143-226-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:33cf:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-8146-d54-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:f2f:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-8152-a91-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:f3e:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-8175-a64-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:33cd:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-8180-583-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:f2e:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-8197-9d8-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:33b1:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-8219-af2-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:33a7:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-8296-d2e-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:33cb:10e:fcc9:0:6ab6"; };
                                                                            { "sas0-8556-f01-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:214f:10e:fcc9:0:6ab6"; };
                                                                            { "sas1-7965-5bb-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3341:10e:fcc9:0:6ab6"; };
                                                                            { "sas1-7966-264-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3322:10e:fcc9:0:6ab6"; };
                                                                            { "sas1-7971-288-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:34ca:10e:fcc9:0:6ab6"; };
                                                                            { "sas1-7972-3bd-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:34c3:10e:fcc9:0:6ab6"; };
                                                                            { "sas1-8690-7a7-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:333d:10e:fcc9:0:6ab6"; };
                                                                            { "sas1-9662-0c2-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3205:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-1868-9df-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:34a3:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-1885-472-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:333c:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-1886-bf0-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3340:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-1915-791-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:34bc:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-3470-799-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1021:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-3471-389-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1025:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-3475-243-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:104a:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-3477-510-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1033:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-3478-8a9-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:103b:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-3481-b76-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:102d:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-3487-b3f-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1027:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-3488-138-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1045:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-3490-68f-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1029:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-3491-c7d-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1023:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-3494-c04-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:103e:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-3495-649-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1026:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-3497-fae-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:102b:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-3500-851-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1028:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-3673-62c-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1035:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-3682-e18-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1043:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-3690-66f-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:102f:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-3702-703-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1024:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-4638-29e-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3342:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-4984-9a7-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c14:5303:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-4991-b1c-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:30b7:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-4993-d75-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:30ac:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-4994-acc-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3092:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-4995-e9b-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3094:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-4998-497-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:30a4:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-5001-d8b-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:30ae:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-5034-6ba-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3083:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-5035-a1b-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3099:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-5038-4a4-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:308b:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-5039-1ba-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:30b1:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-5041-d20-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:30b6:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-5044-429-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3096:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-5046-895-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:30b0:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-5047-eb9-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:309d:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-5052-ddc-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:30ad:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-5053-643-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:30b8:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-5054-206-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:30bc:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-5057-428-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3081:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-5059-c0f-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:308a:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-5062-667-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:30be:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-5065-cd8-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:30a6:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-5088-a31-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3090:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-5092-f45-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3082:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-5097-bc4-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3091:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-5098-6dc-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3093:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-5100-2d5-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3098:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-5103-bfa-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:30af:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-5114-9e6-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:30c5:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-5115-738-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:308c:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-5118-8e4-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3087:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-5269-230-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:2a27:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7485-2d2-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3da2:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7489-b59-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e2e:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7490-460-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e18:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7491-338-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3d82:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7493-152-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3d8f:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7497-c8c-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e1d:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7498-3bc-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e05:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7499-354-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e0a:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7501-2ee-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3d95:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7503-f82-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3d8e:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7513-4ae-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3d93:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7514-2ff-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3d9d:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7515-d3d-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3d8b:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7517-354-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3da4:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7518-627-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3da1:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7523-806-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:40af:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7539-dea-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e04:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7540-7bc-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e20:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7545-fc0-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3d9f:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7546-c4b-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3d90:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7549-26e-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3d9c:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7551-d5a-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3da8:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7554-9dc-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3d86:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7578-8e9-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3d81:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7587-94e-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3da6:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7588-c34-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3dac:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7589-cd1-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e21:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7590-090-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e2f:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7591-d0c-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e16:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7593-b12-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e11:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7594-699-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e2b:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7595-08d-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e19:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7597-5a2-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e0b:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7598-0c1-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e0d:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7605-b38-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3da5:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7607-d23-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e25:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7609-84d-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e26:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7610-9c4-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e12:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7611-e8f-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e13:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7612-6e9-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3dad:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7614-b4b-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e24:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7637-7fd-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3da9:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7638-675-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3d92:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7641-2a6-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e1c:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7643-cc8-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e10:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7644-4e3-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3dae:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7645-27d-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3da0:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7647-a9e-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e28:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7648-8d8-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e1b:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7649-742-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e23:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7650-32c-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e14:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7651-f8c-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e07:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7653-61b-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e27:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7654-e5b-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e2d:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7655-22c-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e17:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7659-d2a-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e0e:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7667-d8d-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e0c:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7671-939-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e0f:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7684-e46-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e06:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7685-53b-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3daf:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7692-82a-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3d85:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7693-74a-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3d88:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7694-ed6-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3d94:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7695-ef6-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3d84:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7697-32b-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3e29:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7698-15e-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3d87:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7962-46f-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:4194:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7963-bcf-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f86:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7965-f95-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:419f:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7966-947-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f84:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7967-575-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:41a0:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7973-429-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3fa6:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7976-6fc-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f91:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7977-2a5-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f83:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7978-95d-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3fa7:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7979-bff-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f87:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7980-9b4-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f95:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7982-58e-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f92:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7983-ff2-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:4192:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7984-053-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f1c:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7985-778-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f00:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7986-1b1-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f06:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7988-6ac-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f10:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7990-d13-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f19:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7991-862-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f25:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7992-80e-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f0e:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7993-a03-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f9d:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7994-dbe-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f94:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7995-0a4-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:4186:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7996-8ce-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:418a:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-7997-ce3-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:4198:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8000-078-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f0a:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8002-e14-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f90:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8003-673-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:418e:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8005-42a-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f26:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8006-8a0-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:41ac:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8007-a01-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f23:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8009-73f-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f80:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8010-f9d-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:41a4:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8011-e2a-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f22:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8012-478-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f99:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8014-be3-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f14:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8016-345-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:419d:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8018-89e-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:4190:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8022-da9-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:4182:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8023-4bd-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f07:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8025-2d1-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f02:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8027-dfe-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:4188:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8030-669-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f15:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8031-6de-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f1d:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8034-1eb-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3fa5:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8036-6e2-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:4189:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8037-e20-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3fa1:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8038-f20-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f2d:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8039-694-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f16:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8041-17f-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f13:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8043-354-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f1f:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8045-007-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f81:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8048-fb6-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:419b:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8049-6b1-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f0c:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8050-211-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:41a7:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8053-189-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:41a3:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8060-3a1-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3fa0:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8062-917-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:419a:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8063-0e3-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:4195:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8065-51d-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f0d:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8067-98e-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3fa4:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8069-16e-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f88:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8070-8d2-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f18:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8071-cf5-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f8b:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8072-616-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f20:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8073-d8e-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f8e:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8074-8da-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:41a6:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8075-c36-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f8c:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8076-131-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:4184:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8077-77c-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f05:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8078-576-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f8a:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8079-04e-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f82:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8082-20d-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3fa3:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8084-52f-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f0b:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8085-797-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f9c:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8086-854-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f12:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8087-978-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:41a5:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8088-bb7-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:418c:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8089-310-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f04:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8091-e13-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f24:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8092-df0-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f0f:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8093-cca-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:418f:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8095-891-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f01:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8096-392-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:419e:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8097-f44-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f08:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8098-30b-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3fa2:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8102-3da-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:4196:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8103-d1d-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f9f:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8105-a90-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f8d:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8106-ac4-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f97:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8107-47f-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:41a1:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8108-d6c-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:4181:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8109-94a-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3fad:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8110-f88-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:4199:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8111-599-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f9a:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8113-71e-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:4193:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8115-3d4-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f1e:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8116-306-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f17:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8118-c86-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:3f93:10e:fcc9:0:6ab6"; };
                                                                            { "sas2-8119-b56-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c11:41a2:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-2579-a17-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:333e:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-2580-672-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:34b6:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-2581-501-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3329:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-2585-f11-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:34b0:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-2589-e35-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:34a4:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-2590-50a-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:34b2:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-2593-1bc-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:32b8:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-2594-ef6-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:32a8:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-2604-b34-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:32af:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-2609-536-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:32c1:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-2614-1bb-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:32c4:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-2626-b19-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:32a6:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-2628-a1d-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:32ab:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-2629-552-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:34ac:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-2648-c84-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:32a7:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-2655-774-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:32a1:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-2659-4da-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3439:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-2661-10b-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:32bf:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-2662-4bb-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:32bd:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-2666-52c-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3431:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-2673-ab1-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:32c7:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-2674-e26-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3440:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-2676-eaa-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:342f:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-2677-34c-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3424:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-2680-9df-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:342e:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-3085-2f7-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3437:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-3089-8df-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:343e:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-3096-5cd-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3436:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-3098-2b4-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:32c5:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-3099-b7b-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:32b9:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-3100-c2a-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3443:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-3128-1ad-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:32bb:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-3151-42d-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:342a:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-3159-532-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:32c2:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-3182-097-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:34c0:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-3185-58a-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:333b:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-3192-e6a-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3333:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-3199-9f7-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3323:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-3207-3c9-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3344:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-3211-baa-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:333a:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-3262-fe8-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:3346:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-3267-b10-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:34a8:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-3278-79c-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:34a7:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-3280-7ae-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:34ae:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-3282-e89-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:34c6:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-3317-39e-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:343b:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-3321-206-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:343c:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-3328-1ea-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:32b2:10e:fcc9:0:6ab6"; };
                                                                            { "sas3-3329-02e-sas-yabs-fronten-95a-27318.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c1c:34c7:10e:fcc9:0:6ab6"; };
                                                                          }, {
                                                                            resolve_timeout = "10ms";
                                                                            connect_timeout = "50ms";
                                                                            backend_timeout = "10s";
                                                                            fail_on_5xx = true;
                                                                            http_backend = true;
                                                                            buffering = false;
                                                                            keepalive_count = 1;
                                                                            need_resolve = true;
                                                                            keepalive_timeout = "60s";
                                                                          }))
                                                                        }; -- rendezvous_hashing
                                                                      }; -- balancer2
                                                                    }; -- report
                                                                  }; -- bs_sas56_meta
                                                                  bs_sas80_meta = {
                                                                    weight = 1.000;
                                                                    report = {
                                                                      uuid = "bs_sas80_meta";
                                                                      ranges = get_str_var("default_ranges");
                                                                      backend_time_ranges = "100ms,150ms,200ms,300ms,500ms";
                                                                      just_storage = false;
                                                                      disable_robotness = true;
                                                                      disable_sslness = true;
                                                                      events = {
                                                                        stats = "report";
                                                                      }; -- events
                                                                      balancer2 = {
                                                                        active_policy = {
                                                                          unique_policy = {};
                                                                        }; -- active_policy
                                                                        attempts = 1;
                                                                        rendezvous_hashing = {
                                                                          unpack(gen_proxy_backends({
                                                                            { "sas0-8418-848-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21c5:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8419-9fb-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bb6:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8420-902-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2145:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8421-17e-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:212b:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8422-b69-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bc5:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8423-2b8-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bab:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8424-c49-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21c1:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8425-4c9-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21c6:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8426-cf8-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14be:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8430-3b8-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21be:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8431-3f6-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:212d:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8432-502-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21cb:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8433-c6e-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bbd:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8434-231-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bb5:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8435-e58-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14c2:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8436-3cd-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21c3:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8437-2da-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2142:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8439-78d-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2129:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8443-e02-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14a3:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8445-f6e-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21bd:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8448-36d-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2131:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8451-42f-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21b4:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8453-509-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2123:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8454-8a5-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:213f:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8455-637-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bce:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8457-57a-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bcc:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8458-3ea-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14a2:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8459-410-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14ae:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8460-aa5-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:213a:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8461-4ae-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14c4:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8462-f0b-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14af:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8463-62f-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21c0:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8464-64a-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2140:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8465-57c-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:214c:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8467-359-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21c9:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8468-1ae-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21b7:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8473-51d-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bcf:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8474-a58-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21a8:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8475-189-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14cc:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8476-c8b-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2130:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8478-1b3-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:212e:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8479-f06-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2125:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8483-2ee-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14a0:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8488-950-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bb1:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8489-626-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14cb:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8491-d79-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21b8:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8492-67f-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14ca:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8493-633-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21a1:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8495-a86-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bad:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8496-ac3-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14ac:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8499-5ce-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14b1:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8500-3c0-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bc6:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8501-512-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2135:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8502-222-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bcd:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8504-902-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bb2:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8506-cc9-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1baf:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8507-269-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:214a:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8508-b2b-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bc0:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8511-153-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bc7:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8513-3c1-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21bb:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8516-d01-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:212c:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8520-1d5-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bbc:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8521-353-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14a4:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8523-215-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bac:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8524-67f-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21ce:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8525-d00-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1ba8:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8527-a56-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2138:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8528-62f-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14c3:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8529-5e7-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bb4:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8532-27f-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2127:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8533-ae1-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bb0:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8534-461-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21af:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8535-707-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2133:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8536-61b-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14c5:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8537-071-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14c8:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8538-3ec-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bbe:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8539-da0-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2143:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8541-3b6-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14c0:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8542-861-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14b6:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8543-38a-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2121:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8545-b69-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21b1:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8546-a5b-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21cd:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8547-d19-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21b6:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8548-eee-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14aa:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8549-eed-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:214d:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8550-3e4-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:213d:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8551-b36-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21ca:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8552-f21-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bb3:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8553-c9f-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2128:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8554-3dd-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:2136:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8555-81f-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14a7:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8558-3da-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21c7:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8559-78c-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21b2:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8561-4d1-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14ba:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8563-057-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21a5:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8564-ff3-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21b3:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8565-991-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14bc:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8566-247-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14b0:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8568-111-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21c8:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8569-6e5-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bc1:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8571-0d6-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14c7:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8572-f5f-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21ab:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8573-558-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21ac:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8575-ef5-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21cf:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8577-c12-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21a7:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8578-52e-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bb9:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8579-c5d-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14bd:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8580-169-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14a5:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8581-7cc-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bb8:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8582-82d-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21b0:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8584-926-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bbb:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8585-3e6-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14a9:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8586-0f5-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21ba:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8587-1ab-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1ba9:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8588-be7-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14b9:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8593-5a5-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14ad:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8595-9cf-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21b5:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8597-a96-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21ae:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8598-0d4-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bbf:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8599-196-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14c9:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8602-71a-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14b5:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8603-455-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bcb:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8604-c26-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14ab:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8607-2ef-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bca:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8613-360-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14c1:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8616-734-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1ba7:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8617-985-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21a3:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8618-8bd-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21cc:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8620-060-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1baa:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8622-6a0-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14b2:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8623-669-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21b9:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8626-fed-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14bf:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8627-5ce-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21bc:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8628-cad-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bb7:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8630-375-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14b3:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8631-2a0-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14b4:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8633-8fd-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21aa:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8634-001-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bc8:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8635-5f3-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1ba5:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8636-889-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1ba6:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8641-608-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:21bf:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8643-745-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bba:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8644-13f-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bc9:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8648-dbc-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:1bc3:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8650-728-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14b7:10e:fcc5:0:5e29"; };
                                                                            { "sas0-8653-eee-sas-yabs-fronten-f5a-24105.gencfg-c.yandex.net"; 8001; 1293.000; "2a02:6b8:c23:14c6:10e:fcc5:0:5e29"; };
                                                                          }, {
                                                                            resolve_timeout = "10ms";
                                                                            connect_timeout = "50ms";
                                                                            backend_timeout = "10s";
                                                                            fail_on_5xx = true;
                                                                            http_backend = true;
                                                                            buffering = false;
                                                                            keepalive_count = 1;
                                                                            need_resolve = true;
                                                                            keepalive_timeout = "60s";
                                                                          }))
                                                                        }; -- rendezvous_hashing
                                                                      }; -- balancer2
                                                                    }; -- report
                                                                  }; -- bs_sas80_meta
                                                                  bs_vla104_meta = {
                                                                    weight = 1.000;
                                                                    report = {
                                                                      uuid = "bs_vla104_meta";
                                                                      ranges = get_str_var("default_ranges");
                                                                      backend_time_ranges = "100ms,150ms,200ms,300ms,500ms";
                                                                      just_storage = false;
                                                                      disable_robotness = true;
                                                                      disable_sslness = true;
                                                                      events = {
                                                                        stats = "report";
                                                                      }; -- events
                                                                      balancer2 = {
                                                                        active_policy = {
                                                                          unique_policy = {};
                                                                        }; -- active_policy
                                                                        attempts = 1;
                                                                        rendezvous_hashing = {
                                                                          unpack(gen_proxy_backends({
                                                                            { "vla1-4752-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:406:10e:fcbe:0:739c"; };
                                                                            { "vla1-4753-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b02:10e:fcbe:0:739c"; };
                                                                            { "vla1-4754-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:419:10e:fcbe:0:739c"; };
                                                                            { "vla1-4755-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:416:10e:fcbe:0:739c"; };
                                                                            { "vla1-4756-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b0a:10e:fcbe:0:739c"; };
                                                                            { "vla1-4757-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b08:10e:fcbe:0:739c"; };
                                                                            { "vla1-4759-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b15:10e:fcbe:0:739c"; };
                                                                            { "vla1-4760-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b0c:10e:fcbe:0:739c"; };
                                                                            { "vla1-4761-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b1a:10e:fcbe:0:739c"; };
                                                                            { "vla1-4762-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b22:10e:fcbe:0:739c"; };
                                                                            { "vla1-4763-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b11:10e:fcbe:0:739c"; };
                                                                            { "vla1-4764-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b0b:10e:fcbe:0:739c"; };
                                                                            { "vla1-4765-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:415:10e:fcbe:0:739c"; };
                                                                            { "vla1-4766-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:401:10e:fcbe:0:739c"; };
                                                                            { "vla1-4767-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b20:10e:fcbe:0:739c"; };
                                                                            { "vla1-4768-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b19:10e:fcbe:0:739c"; };
                                                                            { "vla1-4769-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b16:10e:fcbe:0:739c"; };
                                                                            { "vla1-4770-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b0f:10e:fcbe:0:739c"; };
                                                                            { "vla1-4771-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b10:10e:fcbe:0:739c"; };
                                                                            { "vla1-4772-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:413:10e:fcbe:0:739c"; };
                                                                            { "vla1-4773-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b00:10e:fcbe:0:739c"; };
                                                                            { "vla1-4774-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:403:10e:fcbe:0:739c"; };
                                                                            { "vla1-4776-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b07:10e:fcbe:0:739c"; };
                                                                            { "vla1-4777-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b13:10e:fcbe:0:739c"; };
                                                                            { "vla1-4778-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b0d:10e:fcbe:0:739c"; };
                                                                            { "vla1-4779-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b1e:10e:fcbe:0:739c"; };
                                                                            { "vla1-4782-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b09:10e:fcbe:0:739c"; };
                                                                            { "vla1-4783-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a8f:10e:fcbe:0:739c"; };
                                                                            { "vla1-4784-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b14:10e:fcbe:0:739c"; };
                                                                            { "vla1-4786-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b0e:10e:fcbe:0:739c"; };
                                                                            { "vla1-4787-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b01:10e:fcbe:0:739c"; };
                                                                            { "vla1-4788-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:405:10e:fcbe:0:739c"; };
                                                                            { "vla1-4789-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:410:10e:fcbe:0:739c"; };
                                                                            { "vla1-4791-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:40f:10e:fcbe:0:739c"; };
                                                                            { "vla1-4792-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b1b:10e:fcbe:0:739c"; };
                                                                            { "vla1-4793-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b06:10e:fcbe:0:739c"; };
                                                                            { "vla1-4795-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a83:10e:fcbe:0:739c"; };
                                                                            { "vla1-4798-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b23:10e:fcbe:0:739c"; };
                                                                            { "vla1-4801-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:421:10e:fcbe:0:739c"; };
                                                                            { "vla1-4802-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b18:10e:fcbe:0:739c"; };
                                                                            { "vla1-4804-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b12:10e:fcbe:0:739c"; };
                                                                            { "vla1-4805-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:411:10e:fcbe:0:739c"; };
                                                                            { "vla1-4806-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b1f:10e:fcbe:0:739c"; };
                                                                            { "vla1-4807-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b17:10e:fcbe:0:739c"; };
                                                                            { "vla1-4809-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a8d:10e:fcbe:0:739c"; };
                                                                            { "vla1-4811-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b1d:10e:fcbe:0:739c"; };
                                                                            { "vla1-4814-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:40d:10e:fcbe:0:739c"; };
                                                                            { "vla1-4815-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b05:10e:fcbe:0:739c"; };
                                                                            { "vla1-4816-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:407:10e:fcbe:0:739c"; };
                                                                            { "vla1-4817-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:418:10e:fcbe:0:739c"; };
                                                                            { "vla1-4823-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b1c:10e:fcbe:0:739c"; };
                                                                            { "vla1-4828-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:409:10e:fcbe:0:739c"; };
                                                                            { "vla1-4834-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:7:10e:fcbe:0:739c"; };
                                                                            { "vla1-4835-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:18:10e:fcbe:0:739c"; };
                                                                            { "vla1-4839-vla-yabs-frontend-serve-1c9-29596.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:20:10e:fcbe:0:739c"; };
                                                                          }, {
                                                                            resolve_timeout = "10ms";
                                                                            connect_timeout = "50ms";
                                                                            backend_timeout = "10s";
                                                                            fail_on_5xx = true;
                                                                            http_backend = true;
                                                                            buffering = false;
                                                                            keepalive_count = 1;
                                                                            need_resolve = true;
                                                                            keepalive_timeout = "60s";
                                                                          }))
                                                                        }; -- rendezvous_hashing
                                                                      }; -- balancer2
                                                                    }; -- report
                                                                  }; -- bs_vla104_meta
                                                                  bs_vla56_meta = {
                                                                    weight = 1.000;
                                                                    report = {
                                                                      uuid = "bs_vla56_meta";
                                                                      ranges = get_str_var("default_ranges");
                                                                      backend_time_ranges = "100ms,150ms,200ms,300ms,500ms";
                                                                      just_storage = false;
                                                                      disable_robotness = true;
                                                                      disable_sslness = true;
                                                                      events = {
                                                                        stats = "report";
                                                                      }; -- events
                                                                      balancer2 = {
                                                                        active_policy = {
                                                                          unique_policy = {};
                                                                        }; -- active_policy
                                                                        attempts = 1;
                                                                        rendezvous_hashing = {
                                                                          unpack(gen_proxy_backends({
                                                                            { "vla1-4840-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:21:10e:fcc1:0:3614"; };
                                                                            { "vla1-4846-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:1e:10e:fcc1:0:3614"; };
                                                                            { "vla1-4847-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b21:10e:fcc1:0:3614"; };
                                                                            { "vla1-4848-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:e:10e:fcc1:0:3614"; };
                                                                            { "vla1-4850-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:40b:10e:fcc1:0:3614"; };
                                                                            { "vla1-4851-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:f:10e:fcc1:0:3614"; };
                                                                            { "vla1-4853-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:8c:10e:fcc1:0:3614"; };
                                                                            { "vla1-4855-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:9e:10e:fcc1:0:3614"; };
                                                                            { "vla1-4857-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:0:10e:fcc1:0:3614"; };
                                                                            { "vla1-4858-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:d:10e:fcc1:0:3614"; };
                                                                            { "vla1-4859-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:99:10e:fcc1:0:3614"; };
                                                                            { "vla1-4860-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:96:10e:fcc1:0:3614"; };
                                                                            { "vla1-4862-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:17:10e:fcc1:0:3614"; };
                                                                            { "vla1-4863-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:16:10e:fcc1:0:3614"; };
                                                                            { "vla1-4865-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:1f:10e:fcc1:0:3614"; };
                                                                            { "vla1-4866-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:83:10e:fcc1:0:3614"; };
                                                                            { "vla1-4867-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:22:10e:fcc1:0:3614"; };
                                                                            { "vla1-4868-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:9:10e:fcc1:0:3614"; };
                                                                            { "vla1-4869-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:9b:10e:fcc1:0:3614"; };
                                                                            { "vla1-4870-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:82:10e:fcc1:0:3614"; };
                                                                            { "vla1-4871-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:11c:10e:fcc1:0:3614"; };
                                                                            { "vla1-4874-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:c:10e:fcc1:0:3614"; };
                                                                            { "vla1-4876-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:85:10e:fcc1:0:3614"; };
                                                                            { "vla1-4877-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:94:10e:fcc1:0:3614"; };
                                                                            { "vla1-4878-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:116:10e:fcc1:0:3614"; };
                                                                            { "vla1-4879-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:86:10e:fcc1:0:3614"; };
                                                                            { "vla1-4880-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:10c:10e:fcc1:0:3614"; };
                                                                            { "vla1-4881-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:117:10e:fcc1:0:3614"; };
                                                                            { "vla1-4882-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:8a:10e:fcc1:0:3614"; };
                                                                            { "vla1-4885-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:8f:10e:fcc1:0:3614"; };
                                                                            { "vla1-4886-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:12:10e:fcc1:0:3614"; };
                                                                            { "vla1-4887-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:a3:10e:fcc1:0:3614"; };
                                                                            { "vla1-4890-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:121:10e:fcc1:0:3614"; };
                                                                            { "vla1-4891-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:3:10e:fcc1:0:3614"; };
                                                                            { "vla1-4892-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:89:10e:fcc1:0:3614"; };
                                                                            { "vla1-4893-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:9c:10e:fcc1:0:3614"; };
                                                                            { "vla1-4895-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:a2:10e:fcc1:0:3614"; };
                                                                            { "vla1-4896-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:8d:10e:fcc1:0:3614"; };
                                                                            { "vla1-4898-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:11a:10e:fcc1:0:3614"; };
                                                                            { "vla1-4900-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a98:10e:fcc1:0:3614"; };
                                                                            { "vla1-4901-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:9d:10e:fcc1:0:3614"; };
                                                                            { "vla1-4902-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:a0:10e:fcc1:0:3614"; };
                                                                            { "vla1-4904-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:102:10e:fcc1:0:3614"; };
                                                                            { "vla1-4906-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:11e:10e:fcc1:0:3614"; };
                                                                            { "vla1-4907-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a97:10e:fcc1:0:3614"; };
                                                                            { "vla1-4908-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:107:10e:fcc1:0:3614"; };
                                                                            { "vla1-4909-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a80:10e:fcc1:0:3614"; };
                                                                            { "vla1-4910-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:41e:10e:fcc1:0:3614"; };
                                                                            { "vla1-4912-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:1a:10e:fcc1:0:3614"; };
                                                                            { "vla1-4914-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:10b:10e:fcc1:0:3614"; };
                                                                            { "vla1-4917-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:118:10e:fcc1:0:3614"; };
                                                                            { "vla1-4919-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:98:10e:fcc1:0:3614"; };
                                                                            { "vla1-4925-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a92:10e:fcc1:0:3614"; };
                                                                            { "vla1-4928-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:b:10e:fcc1:0:3614"; };
                                                                            { "vla1-4934-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:95:10e:fcc1:0:3614"; };
                                                                            { "vla1-4935-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:10e:10e:fcc1:0:3614"; };
                                                                            { "vla1-4936-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:115:10e:fcc1:0:3614"; };
                                                                            { "vla1-4937-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a91:10e:fcc1:0:3614"; };
                                                                            { "vla1-4951-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:110:10e:fcc1:0:3614"; };
                                                                            { "vla1-4954-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:11d:10e:fcc1:0:3614"; };
                                                                            { "vla1-4956-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:114:10e:fcc1:0:3614"; };
                                                                            { "vla1-4959-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:1d:10e:fcc1:0:3614"; };
                                                                            { "vla1-4960-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:119:10e:fcc1:0:3614"; };
                                                                            { "vla1-4961-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:87:10e:fcc1:0:3614"; };
                                                                            { "vla1-4962-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:103:10e:fcc1:0:3614"; };
                                                                            { "vla1-4966-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:41c:10e:fcc1:0:3614"; };
                                                                            { "vla1-4968-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a87:10e:fcc1:0:3614"; };
                                                                            { "vla1-4973-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:41a:10e:fcc1:0:3614"; };
                                                                            { "vla1-4975-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:90:10e:fcc1:0:3614"; };
                                                                            { "vla1-4983-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:101:10e:fcc1:0:3614"; };
                                                                            { "vla1-4989-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:10:10e:fcc1:0:3614"; };
                                                                            { "vla1-4990-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2:10e:fcc1:0:3614"; };
                                                                            { "vla1-4992-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a88:10e:fcc1:0:3614"; };
                                                                            { "vla1-4999-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:123:10e:fcc1:0:3614"; };
                                                                            { "vla1-5003-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:a1:10e:fcc1:0:3614"; };
                                                                            { "vla1-5004-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:a:10e:fcc1:0:3614"; };
                                                                            { "vla1-5005-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a95:10e:fcc1:0:3614"; };
                                                                            { "vla1-5008-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2aa1:10e:fcc1:0:3614"; };
                                                                            { "vla1-5009-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:15:10e:fcc1:0:3614"; };
                                                                            { "vla1-5010-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:6:10e:fcc1:0:3614"; };
                                                                            { "vla1-5013-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:88:10e:fcc1:0:3614"; };
                                                                            { "vla1-5016-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:1b:10e:fcc1:0:3614"; };
                                                                            { "vla1-5017-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:106:10e:fcc1:0:3614"; };
                                                                            { "vla1-5020-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:10d:10e:fcc1:0:3614"; };
                                                                            { "vla1-5024-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a85:10e:fcc1:0:3614"; };
                                                                            { "vla1-5026-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:23:10e:fcc1:0:3614"; };
                                                                            { "vla1-5028-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:10a:10e:fcc1:0:3614"; };
                                                                            { "vla1-5030-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a8c:10e:fcc1:0:3614"; };
                                                                            { "vla1-5036-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a9c:10e:fcc1:0:3614"; };
                                                                            { "vla1-5037-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:111:10e:fcc1:0:3614"; };
                                                                            { "vla1-5045-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:80:10e:fcc1:0:3614"; };
                                                                            { "vla1-5046-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:14:10e:fcc1:0:3614"; };
                                                                            { "vla1-5047-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:11:10e:fcc1:0:3614"; };
                                                                            { "vla1-5049-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a96:10e:fcc1:0:3614"; };
                                                                            { "vla1-5052-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a9e:10e:fcc1:0:3614"; };
                                                                            { "vla1-5053-vla-yabs-frontend-serve-96e-13844.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:92:10e:fcc1:0:3614"; };
                                                                          }, {
                                                                            resolve_timeout = "10ms";
                                                                            connect_timeout = "50ms";
                                                                            backend_timeout = "10s";
                                                                            fail_on_5xx = true;
                                                                            http_backend = true;
                                                                            buffering = false;
                                                                            keepalive_count = 1;
                                                                            need_resolve = true;
                                                                            keepalive_timeout = "60s";
                                                                          }))
                                                                        }; -- rendezvous_hashing
                                                                      }; -- balancer2
                                                                    }; -- report
                                                                  }; -- bs_vla56_meta
                                                                  bs_vla64_meta = {
                                                                    weight = 1.000;
                                                                    report = {
                                                                      uuid = "bs_vla64_meta";
                                                                      ranges = get_str_var("default_ranges");
                                                                      backend_time_ranges = "100ms,150ms,200ms,300ms,500ms";
                                                                      just_storage = false;
                                                                      disable_robotness = true;
                                                                      disable_sslness = true;
                                                                      events = {
                                                                        stats = "report";
                                                                      }; -- events
                                                                      balancer2 = {
                                                                        active_policy = {
                                                                          unique_policy = {};
                                                                        }; -- active_policy
                                                                        attempts = 1;
                                                                        rendezvous_hashing = {
                                                                          unpack(gen_proxy_backends({
                                                                            { "vla1-5054-vla-yabs-frontend-serve-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:122:10e:fcc2:0:4b53"; };
                                                                            { "vla1-5055-vla-yabs-frontend-serve-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:1c:10e:fcc2:0:4b53"; };
                                                                            { "vla1-5057-vla-yabs-frontend-serve-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a8e:10e:fcc2:0:4b53"; };
                                                                            { "vla1-5060-vla-yabs-frontend-serve-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:19:10e:fcc2:0:4b53"; };
                                                                            { "vla1-5064-vla-yabs-frontend-serve-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:1:10e:fcc2:0:4b53"; };
                                                                            { "vla1-5065-vla-yabs-frontend-serve-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:4:10e:fcc2:0:4b53"; };
                                                                            { "vla1-5066-vla-yabs-frontend-serve-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:5:10e:fcc2:0:4b53"; };
                                                                            { "vla1-5067-vla-yabs-frontend-serve-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:9a:10e:fcc2:0:4b53"; };
                                                                            { "vla1-5069-vla-yabs-frontend-serve-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:8:10e:fcc2:0:4b53"; };
                                                                            { "vla1-5070-vla-yabs-frontend-serve-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:109:10e:fcc2:0:4b53"; };
                                                                            { "vla1-5071-vla-yabs-frontend-serve-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a81:10e:fcc2:0:4b53"; };
                                                                            { "vla1-5072-vla-yabs-frontend-serve-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2aa2:10e:fcc2:0:4b53"; };
                                                                            { "vla1-5073-vla-yabs-frontend-serve-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:414:10e:fcc2:0:4b53"; };
                                                                            { "vla1-5132-vla-yabs-frontend-serve-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0f:111:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1500-38d-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3f1f:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1502-106-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3f0a:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1517-93d-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3f15:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1519-a24-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3f21:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1520-605-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3f09:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1521-e7a-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3f27:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1522-1e0-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3f17:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1523-3d6-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3f0c:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1533-ea1-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3f00:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1545-0b7-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3f04:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1548-bcc-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3f29:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1549-dff-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3d8e:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1551-4d8-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3d8c:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1556-5c1-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3d92:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1559-d45-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3d9c:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1560-1c5-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3d96:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1561-17e-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3d9a:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1567-cbc-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3daa:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1568-696-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3e9f:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1574-98c-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3da6:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1575-72c-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3da2:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1576-cc0-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3d9b:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1584-a99-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3da8:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1592-d34-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3da4:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1593-81c-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3f07:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1599-a60-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3d81:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1610-f50-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3e83:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1612-955-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3e99:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1613-a74-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3f10:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1616-b50-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3f0e:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1617-3a3-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3d07:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1621-4ce-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3f18:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1724-284-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3f24:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1729-b64-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3f1a:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1734-cf6-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3f1c:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1739-72a-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3f1d:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1742-9f2-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3f08:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1743-d17-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3d27:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1750-b17-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3f06:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1751-be3-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3f28:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1753-2b5-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3f14:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1924-e80-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3f16:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1926-a96-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3f20:10e:fcc2:0:4b53"; };
                                                                            { "vla2-1929-1ea-vla-yabs-fronten-a60-19283.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c1d:3f26:10e:fcc2:0:4b53"; };
                                                                          }, {
                                                                            resolve_timeout = "10ms";
                                                                            connect_timeout = "50ms";
                                                                            backend_timeout = "10s";
                                                                            fail_on_5xx = true;
                                                                            http_backend = true;
                                                                            buffering = false;
                                                                            keepalive_count = 1;
                                                                            need_resolve = true;
                                                                            keepalive_timeout = "60s";
                                                                          }))
                                                                        }; -- rendezvous_hashing
                                                                      }; -- balancer2
                                                                    }; -- report
                                                                  }; -- bs_vla64_meta
                                                                  bs_vla80_meta = {
                                                                    weight = 1.000;
                                                                    report = {
                                                                      uuid = "bs_vla80_meta";
                                                                      ranges = get_str_var("default_ranges");
                                                                      backend_time_ranges = "100ms,150ms,200ms,300ms,500ms";
                                                                      just_storage = false;
                                                                      disable_robotness = true;
                                                                      disable_sslness = true;
                                                                      events = {
                                                                        stats = "report";
                                                                      }; -- events
                                                                      balancer2 = {
                                                                        active_policy = {
                                                                          unique_policy = {};
                                                                        }; -- active_policy
                                                                        attempts = 1;
                                                                        rendezvous_hashing = {
                                                                          unpack(gen_proxy_backends({
                                                                            { "vla1-0083-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:3920:10e:fcbd:0:690a"; };
                                                                            { "vla1-0174-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:4c06:10e:fcbd:0:690a"; };
                                                                            { "vla1-0564-980-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c18:140a:10e:fcbd:0:690a"; };
                                                                            { "vla1-0770-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:220f:10e:fcbd:0:690a"; };
                                                                            { "vla1-0774-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2217:10e:fcbd:0:690a"; };
                                                                            { "vla1-0780-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2683:10e:fcbd:0:690a"; };
                                                                            { "vla1-0936-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:379f:10e:fcbd:0:690a"; };
                                                                            { "vla1-0973-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2194:10e:fcbd:0:690a"; };
                                                                            { "vla1-1029-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:3897:10e:fcbd:0:690a"; };
                                                                            { "vla1-1063-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2693:10e:fcbd:0:690a"; };
                                                                            { "vla1-1092-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a12:10e:fcbd:0:690a"; };
                                                                            { "vla1-1226-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:21a0:10e:fcbd:0:690a"; };
                                                                            { "vla1-1411-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:261d:10e:fcbd:0:690a"; };
                                                                            { "vla1-1511-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2203:10e:fcbd:0:690a"; };
                                                                            { "vla1-1521-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2209:10e:fcbd:0:690a"; };
                                                                            { "vla1-1774-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:388e:10e:fcbd:0:690a"; };
                                                                            { "vla1-1817-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:229b:10e:fcbd:0:690a"; };
                                                                            { "vla1-1818-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:229c:10e:fcbd:0:690a"; };
                                                                            { "vla1-1881-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:3898:10e:fcbd:0:690a"; };
                                                                            { "vla1-1883-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:388c:10e:fcbd:0:690a"; };
                                                                            { "vla1-1993-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2196:10e:fcbd:0:690a"; };
                                                                            { "vla1-2193-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2286:10e:fcbd:0:690a"; };
                                                                            { "vla1-2736-d41-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:112:10e:fcbd:0:690a"; };
                                                                            { "vla1-2737-fbc-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:186:10e:fcbd:0:690a"; };
                                                                            { "vla1-2738-ad3-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0f:218:10e:fcbd:0:690a"; };
                                                                            { "vla1-2739-f46-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0f:222:10e:fcbd:0:690a"; };
                                                                            { "vla1-2741-64a-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0f:200:10e:fcbd:0:690a"; };
                                                                            { "vla1-2742-b6e-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a90:10e:fcbd:0:690a"; };
                                                                            { "vla1-2743-818-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:11b:10e:fcbd:0:690a"; };
                                                                            { "vla1-2744-248-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:399:10e:fcbd:0:690a"; };
                                                                            { "vla1-2745-d12-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a86:10e:fcbd:0:690a"; };
                                                                            { "vla1-2746-ba2-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:19e:10e:fcbd:0:690a"; };
                                                                            { "vla1-2747-7b5-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a9a:10e:fcbd:0:690a"; };
                                                                            { "vla1-2748-614-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:195:10e:fcbd:0:690a"; };
                                                                            { "vla1-2749-cf2-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0f:208:10e:fcbd:0:690a"; };
                                                                            { "vla1-2751-432-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:8b:10e:fcbd:0:690a"; };
                                                                            { "vla1-2752-97f-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a82:10e:fcbd:0:690a"; };
                                                                            { "vla1-2753-be5-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:3a1:10e:fcbd:0:690a"; };
                                                                            { "vla1-2754-b9c-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:108:10e:fcbd:0:690a"; };
                                                                            { "vla1-2755-2a0-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0f:214:10e:fcbd:0:690a"; };
                                                                            { "vla1-2757-b81-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0f:203:10e:fcbd:0:690a"; };
                                                                            { "vla1-2758-cc0-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0f:204:10e:fcbd:0:690a"; };
                                                                            { "vla1-2761-6c2-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0f:206:10e:fcbd:0:690a"; };
                                                                            { "vla1-2762-2d5-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0f:220:10e:fcbd:0:690a"; };
                                                                            { "vla1-2763-991-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:10f:10e:fcbd:0:690a"; };
                                                                            { "vla1-2764-9cd-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:412:10e:fcbd:0:690a"; };
                                                                            { "vla1-2765-539-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:19b:10e:fcbd:0:690a"; };
                                                                            { "vla1-2766-eb0-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0f:223:10e:fcbd:0:690a"; };
                                                                            { "vla1-2767-89f-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a9d:10e:fcbd:0:690a"; };
                                                                            { "vla1-2768-82a-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0f:210:10e:fcbd:0:690a"; };
                                                                            { "vla1-2771-3fe-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a93:10e:fcbd:0:690a"; };
                                                                            { "vla1-2772-853-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:393:10e:fcbd:0:690a"; };
                                                                            { "vla1-2773-65d-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0f:202:10e:fcbd:0:690a"; };
                                                                            { "vla1-2775-f43-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0f:216:10e:fcbd:0:690a"; };
                                                                            { "vla1-2776-d4d-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:196:10e:fcbd:0:690a"; };
                                                                            { "vla1-2777-371-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a94:10e:fcbd:0:690a"; };
                                                                            { "vla1-2778-c9c-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:81:10e:fcbd:0:690a"; };
                                                                            { "vla1-2779-8f0-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:11f:10e:fcbd:0:690a"; };
                                                                            { "vla1-2780-511-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0f:21f:10e:fcbd:0:690a"; };
                                                                            { "vla1-2781-4d2-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0f:20c:10e:fcbd:0:690a"; };
                                                                            { "vla1-2782-261-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0f:212:10e:fcbd:0:690a"; };
                                                                            { "vla1-2783-5d3-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0f:21d:10e:fcbd:0:690a"; };
                                                                            { "vla1-2784-40b-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a89:10e:fcbd:0:690a"; };
                                                                            { "vla1-2785-4aa-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:1a3:10e:fcbd:0:690a"; };
                                                                            { "vla1-2787-e95-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:113:10e:fcbd:0:690a"; };
                                                                            { "vla1-2788-046-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:120:10e:fcbd:0:690a"; };
                                                                            { "vla1-2789-510-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2a8a:10e:fcbd:0:690a"; };
                                                                            { "vla1-2791-43a-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0f:21c:10e:fcbd:0:690a"; };
                                                                            { "vla1-2792-88b-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:39b:10e:fcbd:0:690a"; };
                                                                            { "vla1-2801-f01-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:422:10e:fcbd:0:690a"; };
                                                                            { "vla1-2802-0d9-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:105:10e:fcbd:0:690a"; };
                                                                            { "vla1-2803-2cb-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0f:20a:10e:fcbd:0:690a"; };
                                                                            { "vla1-2806-e46-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0f:221:10e:fcbd:0:690a"; };
                                                                            { "vla1-2807-305-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0f:20e:10e:fcbd:0:690a"; };
                                                                            { "vla1-2808-bd2-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2aa3:10e:fcbd:0:690a"; };
                                                                            { "vla1-2811-f30-vla-yabs-fronten-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:104:10e:fcbd:0:690a"; };
                                                                            { "vla1-3553-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:500c:10e:fcbd:0:690a"; };
                                                                            { "vla1-3606-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:5094:10e:fcbd:0:690a"; };
                                                                            { "vla1-3610-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:5083:10e:fcbd:0:690a"; };
                                                                            { "vla1-3619-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:50a3:10e:fcbd:0:690a"; };
                                                                            { "vla1-3626-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:318f:10e:fcbd:0:690a"; };
                                                                            { "vla1-3632-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2303:10e:fcbd:0:690a"; };
                                                                            { "vla1-3635-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2309:10e:fcbd:0:690a"; };
                                                                            { "vla1-3664-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:3914:10e:fcbd:0:690a"; };
                                                                            { "vla1-3671-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:3280:10e:fcbd:0:690a"; };
                                                                            { "vla1-3672-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:319a:10e:fcbd:0:690a"; };
                                                                            { "vla1-3693-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:3909:10e:fcbd:0:690a"; };
                                                                            { "vla1-3698-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:3298:10e:fcbd:0:690a"; };
                                                                            { "vla1-3720-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:31a3:10e:fcbd:0:690a"; };
                                                                            { "vla1-3726-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:230a:10e:fcbd:0:690a"; };
                                                                            { "vla1-3753-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:5003:10e:fcbd:0:690a"; };
                                                                            { "vla1-3996-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:5019:10e:fcbd:0:690a"; };
                                                                            { "vla1-4145-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:84:10e:fcbd:0:690a"; };
                                                                            { "vla1-4588-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:400:10e:fcbd:0:690a"; };
                                                                            { "vla1-4589-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:40a:10e:fcbd:0:690a"; };
                                                                            { "vla1-4740-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:41b:10e:fcbd:0:690a"; };
                                                                            { "vla1-4741-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:13:10e:fcbd:0:690a"; };
                                                                            { "vla1-4742-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:40e:10e:fcbd:0:690a"; };
                                                                            { "vla1-4743-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:41f:10e:fcbd:0:690a"; };
                                                                            { "vla1-4744-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:408:10e:fcbd:0:690a"; };
                                                                            { "vla1-4745-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:417:10e:fcbd:0:690a"; };
                                                                            { "vla1-4746-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:40c:10e:fcbd:0:690a"; };
                                                                            { "vla1-4747-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:402:10e:fcbd:0:690a"; };
                                                                            { "vla1-4748-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:41d:10e:fcbd:0:690a"; };
                                                                            { "vla1-4749-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b04:10e:fcbd:0:690a"; };
                                                                            { "vla1-4750-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:2b03:10e:fcbd:0:690a"; };
                                                                            { "vla1-4751-vla-yabs-frontend-serve-73c-26890.gencfg-c.yandex.net"; 8001; 2042.000; "2a02:6b8:c0d:420:10e:fcbd:0:690a"; };
                                                                          }, {
                                                                            resolve_timeout = "10ms";
                                                                            connect_timeout = "50ms";
                                                                            backend_timeout = "10s";
                                                                            fail_on_5xx = true;
                                                                            http_backend = true;
                                                                            buffering = false;
                                                                            keepalive_count = 1;
                                                                            need_resolve = true;
                                                                            keepalive_timeout = "60s";
                                                                          }))
                                                                        }; -- rendezvous_hashing
                                                                      }; -- balancer2
                                                                    }; -- report
                                                                  }; -- bs_vla80_meta
                                                                }; -- rendezvous_hashing
                                                              }; -- balancer2
                                                            }; -- shared
                                                          }; -- hasher
                                                        }; -- report
                                                      }; -- client_retry
                                                      adfox = {
                                                        priority = 3;
                                                        match_fsm = {
                                                          path = "/+adfox.*";
                                                          case_insensitive = true;
                                                          surround = false;
                                                        }; -- match_fsm
                                                        report = {
                                                          uuid = "adfox_requests";
                                                          ranges = get_str_var("default_ranges");
                                                          just_storage = false;
                                                          disable_robotness = true;
                                                          disable_sslness = true;
                                                          events = {
                                                            stats = "report";
                                                          }; -- events
                                                          hasher = {
                                                            mode = "random";
                                                            shared = {
                                                              uuid = "586509910830533953";
                                                              shared = {
                                                                uuid = "yabs_frontend_backends";
                                                              }; -- shared
                                                            }; -- shared
                                                          }; -- hasher
                                                        }; -- report
                                                      }; -- adfox
                                                      balance_by_yandexuid = {
                                                        priority = 2;
                                                        match_fsm = {
                                                          cookie = "yandexuid=.*";
                                                          case_insensitive = true;
                                                          surround = false;
                                                        }; -- match_fsm
                                                        report = {
                                                          uuid = "yandexuid_requests";
                                                          ranges = get_str_var("default_ranges");
                                                          just_storage = false;
                                                          disable_robotness = true;
                                                          disable_sslness = true;
                                                          events = {
                                                            stats = "report";
                                                          }; -- events
                                                          cookie_hasher = {
                                                            cookie = "yandexuid";
                                                            file_switch = "./controls/disable_cookie_hasher";
                                                            shared = {
                                                              uuid = "586509910830533953";
                                                            }; -- shared
                                                          }; -- cookie_hasher
                                                        }; -- report
                                                      }; -- balance_by_yandexuid
                                                      default = {
                                                        priority = 1;
                                                        report = {
                                                          uuid = "other_requests";
                                                          ranges = get_str_var("default_ranges");
                                                          just_storage = false;
                                                          disable_robotness = true;
                                                          disable_sslness = true;
                                                          events = {
                                                            stats = "report";
                                                          }; -- events
                                                          hdrcgi = {
                                                            hdr_from_cgi = {
                                                              ["X-Ad-Session-Id"] = "ad-session-id";
                                                            }; -- hdr_from_cgi
                                                            headers_hasher = {
                                                              header_name = "X-Ad-Session-Id";
                                                              surround = false;
                                                              randomize_empty_match = true;
                                                              shared = {
                                                                uuid = "586509910830533953";
                                                              }; -- shared
                                                            }; -- headers_hasher
                                                          }; -- hdrcgi
                                                        }; -- report
                                                      }; -- default
                                                    }; -- regexp
                                                  }; -- hdrcgi
                                                }; -- request_replier
                                              }; -- compressor
                                            }; -- response_headers
                                          }; -- headers
                                        }; -- shared
                                      }; -- report
                                    }; -- shared
                                  }; -- default
                                }; -- regexp
                              }; -- shared
                            }; -- ["an.webvisor.org"]
                            ["jstracer.yandex.ru"] = {
                              priority = 4;
                              pattern = "((jstracer\\.yandex\\.ru))(:80)?";
                              case_insensitive = true;
                              shared = {
                                uuid = "1847316178606023523";
                              }; -- shared
                            }; -- ["jstracer.yandex.ru"]
                            ["statchecker.ru"] = {
                              priority = 3;
                              pattern = "((statchecker\\.ru)|(web-metrica\\.yandex\\.ru)|(uptime-info\\.yandex\\.ru)|(site-status\\.yandex\\.ru)|(visit-monitor\\.yandex\\.ru)|(statchecker\\.yandex\\.ru)|(site-ping\\.yandex\\.ru))(:80)?";
                              case_insensitive = true;
                              shared = {
                                uuid = "1847316178606023523";
                              }; -- shared
                            }; -- ["statchecker.ru"]
                            ["verify.yandex.ru"] = {
                              priority = 2;
                              pattern = "((verify\\.yandex\\.ru)|([^.]+\\.verify\\.yandex\\.ru))(:80)?";
                              case_insensitive = true;
                              shared = {
                                uuid = "1847316178606023523";
                              }; -- shared
                            }; -- ["verify.yandex.ru"]
                            default = {
                              
                              shared = {
                                uuid = "2831928262244888137";
                                regexp = {
                                  default = {
                                    priority = 1;
                                    regexp = {
                                      legacy_metrika_redirect = {
                                        priority = 6;
                                        match_or = {
                                          {
                                            match_fsm = {
                                              url = "/+watch/.*";
                                              case_insensitive = false;
                                              surround = false;
                                            }; -- match_fsm
                                          };
                                          {
                                            match_fsm = {
                                              url = "/+informer/.*";
                                              case_insensitive = false;
                                              surround = false;
                                            }; -- match_fsm
                                          };
                                        }; -- match_or
                                        shared = {
                                          uuid = "243030124386208781";
                                        }; -- shared
                                      }; -- legacy_metrika_redirect
                                      ["partner-code-bundles"] = {
                                        priority = 5;
                                        match_or = {
                                          {
                                            match_fsm = {
                                              path = "/+resource/+context_static_r_\\d+\\.js";
                                              case_insensitive = false;
                                              surround = false;
                                            }; -- match_fsm
                                          };
                                          {
                                            match_fsm = {
                                              path = "/+partner-code-bundles/+\\d+/.*";
                                              case_insensitive = false;
                                              surround = false;
                                            }; -- match_fsm
                                          };
                                        }; -- match_or
                                        report = {
                                          uuid = "partner-code-bundles";
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
                                                regexp = "^/+resource/+context_static_r_(\\d+)\\.js";
                                                rewrite = "/partner-code-bundles/%1/context_static.js";
                                              };
                                            }; -- actions
                                            shared = {
                                              uuid = "proxy_yastatic";
                                              headers = {
                                                create_func_weak = {
                                                  ["X-Real-IP"] = "realip";
                                                }; -- create_func_weak
                                                create = {
                                                  Host = "yastatic.net";
                                                }; -- create
                                                create_weak = {
                                                  ["X-Forwarded-Proto"] = "https";
                                                }; -- create_weak
                                                response_headers = {
                                                  create = {
                                                    ["Strict-Transport-Security"] = "max-age=31536000";
                                                  }; -- create
                                                  balancer2 = {
                                                    simple_policy = {};
                                                    attempts = 2;
                                                    rr = {
                                                      unpack(gen_proxy_backends({
                                                        { "yastatic.net"; 80; 1.000; "2a02:6b8:20::215"; };
                                                      }, {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "50ms";
                                                        backend_timeout = "500ms";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = true;
                                                      }))
                                                    }; -- rr
                                                  }; -- balancer2
                                                }; -- response_headers
                                              }; -- headers
                                            }; -- shared
                                          }; -- rewrite
                                        }; -- report
                                      }; -- ["partner-code-bundles"]
                                      pcode_redirect = {
                                        priority = 4;
                                        match_or = {
                                          {
                                            match_fsm = {
                                              path = "/+resource/+context\\.js";
                                              case_insensitive = false;
                                              surround = false;
                                            }; -- match_fsm
                                          };
                                          {
                                            match_fsm = {
                                              path = "/+system/.+\\.js";
                                              case_insensitive = false;
                                              surround = false;
                                            }; -- match_fsm
                                          };
                                          {
                                            match_fsm = {
                                              path = "/+(resource|system)-debug/.+";
                                              case_insensitive = false;
                                              surround = false;
                                            }; -- match_fsm
                                          };
                                        }; -- match_or
                                        report = {
                                          uuid = "pcode_redirect";
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
                                                rewrite = "/pcode?route=context";
                                                regexp = "^/+resource/+context\\.js";
                                              };
                                              {
                                                global = false;
                                                literal = false;
                                                case_insensitive = false;
                                                rewrite = "/pcode?route=%1";
                                                regexp = "^/+system/+(.+)\\.js";
                                              };
                                              {
                                                global = false;
                                                literal = false;
                                                case_insensitive = false;
                                                rewrite = "/pcode?route=%2";
                                                regexp = "^/+(resource|system)-debug/+(.+)";
                                              };
                                            }; -- actions
                                            shared = {
                                              uuid = "proxy_pcode";
                                              headers = {
                                                create_func_weak = {
                                                  ["X-Real-IP"] = "realip";
                                                }; -- create_func_weak
                                                create = {
                                                  Host = "pcode-static.yabs.yandex.net";
                                                }; -- create
                                                create_weak = {
                                                  ["X-Forwarded-Proto"] = "https";
                                                }; -- create_weak
                                                response_headers = {
                                                  create = {
                                                    ["Strict-Transport-Security"] = "max-age=31536000";
                                                  }; -- create
                                                  balancer2 = {
                                                    simple_policy = {};
                                                    attempts = 2;
                                                    rr = {
                                                      unpack(gen_proxy_backends({
                                                        { "pcode-static.yabs.yandex.net"; 80; 1.000; "2a02:6b8:0:3400:0:71d:0:3da"; };
                                                      }, {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "50ms";
                                                        backend_timeout = "10s";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = true;
                                                      }))
                                                    }; -- rr
                                                  }; -- balancer2
                                                }; -- response_headers
                                              }; -- headers
                                            }; -- shared
                                          }; -- rewrite
                                        }; -- report
                                      }; -- pcode_redirect
                                      jstracer = {
                                        priority = 3;
                                        match_fsm = {
                                          url = "/+jstracer";
                                          case_insensitive = false;
                                          surround = false;
                                        }; -- match_fsm
                                        shared = {
                                          uuid = "4544929115444722299";
                                        }; -- shared
                                      }; -- jstracer
                                      base_info_restrict = {
                                        priority = 2;
                                        match_or = {
                                          {
                                            match_fsm = {
                                              path = "/+base_info(/.*)?";
                                              case_insensitive = true;
                                              surround = false;
                                            }; -- match_fsm
                                          };
                                          {
                                            match_fsm = {
                                              path = "/+meta_info(/.*)?";
                                              case_insensitive = true;
                                              surround = false;
                                            }; -- match_fsm
                                          };
                                          {
                                            match_fsm = {
                                              path = "/+status(/.*)?";
                                              case_insensitive = true;
                                              surround = false;
                                            }; -- match_fsm
                                          };
                                        }; -- match_or
                                        shared = {
                                          uuid = "3149712711745469625";
                                        }; -- shared
                                      }; -- base_info_restrict
                                      default = {
                                        priority = 1;
                                        shared = {
                                          uuid = "3357443050276055493";
                                        }; -- shared
                                      }; -- default
                                    }; -- regexp
                                  }; -- default
                                }; -- regexp
                              }; -- shared
                            }; -- default
                          }; -- regexp_host
                        }; -- default
                      }; -- regexp
                    }; -- headers
                  }; -- default
                }; -- regexp
              }; -- cookie_policy
            }; -- report
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
          http2_alpn_file = "./controls/http2_enable.ratefile";
          http2_alpn_freq = 1.000;
          ja3_enabled = true;
          contexts = {
            ["an.webvisor.org"] = {
              priority = 5;
              timeout = "100800s";
              disable_sslv3 = true;
              ciphers = get_str_var("default_ciphers");
              log = get_log_path("ssl_sni", 443, "/place/db/www/logs");
              priv = get_private_cert_path("an.webvisor.org.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-an.webvisor.org.pem", "/dev/shm/balancer");
              servername = {
                surround = false;
                case_insensitive = false;
                servername_regexp = "(an\\.webvisor\\.com)|(an\\.webvisor\\.org)";
              }; -- servername
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.an.webvisor.org.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.an.webvisor.org.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.an.webvisor.org.key", "/dev/shm/balancer/priv");
                };
              }; -- ticket_keys_list
            }; -- ["an.webvisor.org"]
            ["jstracer.yandex.ru"] = {
              priority = 4;
              timeout = "100800s";
              disable_sslv3 = true;
              ciphers = get_str_var("default_ciphers");
              log = get_log_path("ssl_sni", 443, "/place/db/www/logs");
              priv = get_private_cert_path("jstracer.yandex.ru.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-jstracer.yandex.ru.pem", "/dev/shm/balancer");
              servername = {
                surround = false;
                case_insensitive = false;
                servername_regexp = "(jstracer\\.yandex\\.ru)";
              }; -- servername
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.jstracer.yandex.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.jstracer.yandex.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.jstracer.yandex.ru.key", "/dev/shm/balancer/priv");
                };
              }; -- ticket_keys_list
            }; -- ["jstracer.yandex.ru"]
            ["statchecker.yandex.ru"] = {
              priority = 3;
              timeout = "100800s";
              disable_sslv3 = true;
              ciphers = get_str_var("default_ciphers");
              log = get_log_path("ssl_sni", 443, "/place/db/www/logs");
              priv = get_private_cert_path("statchecker.yandex.ru.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-statchecker.yandex.ru.pem", "/dev/shm/balancer");
              servername = {
                surround = false;
                case_insensitive = false;
                servername_regexp = "(statchecker\\.ru)|(web-metrica\\.yandex\\.ru)|(uptime-info\\.yandex\\.ru)|(site-status\\.yandex\\.ru)|(visit-monitor\\.yandex\\.ru)|(statchecker\\.yandex\\.ru)|(site-ping\\.yandex\\.ru)";
              }; -- servername
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.statchecker.yandex.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.statchecker.yandex.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.statchecker.yandex.ru.key", "/dev/shm/balancer/priv");
                };
              }; -- ticket_keys_list
            }; -- ["statchecker.yandex.ru"]
            ["verify.yandex.ru"] = {
              priority = 2;
              timeout = "100800s";
              disable_sslv3 = true;
              ciphers = get_str_var("default_ciphers");
              log = get_log_path("ssl_sni", 443, "/place/db/www/logs");
              priv = get_private_cert_path("verify.yandex.ru.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-verify.yandex.ru.pem", "/dev/shm/balancer");
              servername = {
                surround = false;
                case_insensitive = false;
                servername_regexp = "(verify\\.yandex\\.ru)|([^.]+\\.verify\\.yandex\\.ru)";
              }; -- servername
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.verify.yandex.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.verify.yandex.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.verify.yandex.ru.key", "/dev/shm/balancer/priv");
                };
              }; -- ticket_keys_list
            }; -- ["verify.yandex.ru"]
            default = {
              priority = 1;
              timeout = "100800s";
              disable_sslv3 = true;
              log = get_log_path("ssl_sni", 443, "/place/db/www/logs");
              priv = get_private_cert_path("bs.yandex.ru.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-bs.yandex.ru.pem", "/dev/shm/balancer");
              secondary = {
                priv = get_private_cert_path("bs.yandex.ru_RSA.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-bs.yandex.ru_RSA.pem", "/dev/shm/balancer");
              }; -- secondary
              ciphers = "ECDHE-ECDSA-AES128-GCM-SHA256:kEECDH+AESGCM+AES128:kEECDH+AES128:kEECDH+AESGCM+AES256:kRSA+AESGCM+AES128:kRSA+AES128:RC4-SHA:DES-CBC3-SHA:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.bs.yandex.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.bs.yandex.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.bs.yandex.ru.key", "/dev/shm/balancer/priv");
                };
              }; -- ticket_keys_list
            }; -- default
          }; -- contexts
          http2 = {
            goaway_debug_data_enabled = false;
            debug_log_enabled = false;
            events = {
              stats = "report";
            }; -- events
            http = {
              maxlen = 65536;
              maxreq = 65536;
              keepalive = true;
              no_keepalive_file = "./controls/keepalive_disabled";
              keepalive_drop_probability = 0.010;
              events = {
                stats = "report";
              }; -- events
              accesslog = {
                log = get_log_path("access_log", 443, "/place/db/www/logs");
                report = {
                  ranges = get_str_var("default_ranges");
                  input_size_ranges = "32,64,128,256,512,1024,4096,8192,16384,131072,524288,1048576,2097152";
                  output_size_ranges = "512,1024,4096,8192,16384,32768,65536,131072,262144,524288,1048576,2097152,4194304,8388608";
                  just_storage = false;
                  disable_robotness = true;
                  disable_sslness = true;
                  events = {
                    stats = "report";
                  }; -- events
                  cookie_policy = {
                    uuid = "service_total";
                    default_yandex_policies = "stable";
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
                          url = "/ping";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        shared = {
                          uuid = "1683180570280210434";
                        }; -- shared
                      }; -- slbping
                      default = {
                        priority = 1;
                        headers = {
                          create = {
                            ["X-Yabs-Balancer-Samogon-Key"] = "101";
                          }; -- create
                          regexp = {
                            default = {
                              priority = 1;
                              regexp_host = {
                                ["an.webvisor.org"] = {
                                  priority = 5;
                                  pattern = "((an\\.webvisor\\.com)|(an\\.webvisor\\.org))(:443)?";
                                  case_insensitive = true;
                                  shared = {
                                    uuid = "1847316178606023523";
                                  }; -- shared
                                }; -- ["an.webvisor.org"]
                                ["jstracer.yandex.ru"] = {
                                  priority = 4;
                                  pattern = "((jstracer\\.yandex\\.ru))(:443)?";
                                  case_insensitive = true;
                                  shared = {
                                    uuid = "1847316178606023523";
                                  }; -- shared
                                }; -- ["jstracer.yandex.ru"]
                                ["statchecker.ru"] = {
                                  priority = 3;
                                  pattern = "((statchecker\\.ru)|(web-metrica\\.yandex\\.ru)|(uptime-info\\.yandex\\.ru)|(site-status\\.yandex\\.ru)|(visit-monitor\\.yandex\\.ru)|(statchecker\\.yandex\\.ru)|(site-ping\\.yandex\\.ru))(:443)?";
                                  case_insensitive = true;
                                  shared = {
                                    uuid = "1847316178606023523";
                                  }; -- shared
                                }; -- ["statchecker.ru"]
                                ["verify.yandex.ru"] = {
                                  priority = 2;
                                  pattern = "((verify\\.yandex\\.ru)|([^.]+\\.verify\\.yandex\\.ru))(:443)?";
                                  case_insensitive = true;
                                  shared = {
                                    uuid = "1847316178606023523";
                                  }; -- shared
                                }; -- ["verify.yandex.ru"]
                                default = {
                                  
                                  shared = {
                                    uuid = "2831928262244888137";
                                  }; -- shared
                                }; -- default
                              }; -- regexp_host
                            }; -- default
                          }; -- regexp
                        }; -- headers
                      }; -- default
                    }; -- regexp
                  }; -- cookie_policy
                  refers = "service_total";
                }; -- report
              }; -- accesslog
            }; -- http
          }; -- http2
        }; -- ssl_sni
      }; -- errorlog
    }; -- https_section
  }; -- ipdispatch
}