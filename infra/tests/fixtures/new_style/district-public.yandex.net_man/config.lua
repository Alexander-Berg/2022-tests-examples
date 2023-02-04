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
  unistat = {
    addrs = {
      {
        ip = "*";
        port = get_port_var("port", 2);
        disabled = get_int_var("disable_external", 0);
      };
    }; -- addrs
  }; -- unistat
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
  sd = {
    client_name = "awacs-l7-balancer(district-public.yandex.net:district-public.yandex.net_man)";
    host = "sd.yandex.net";
    port = 8080;
    connect_timeout = "50ms";
    request_timeout = "1s";
    cache_dir = "./sd_cache";
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
          events = {
            stats = "report";
          }; -- events
          accesslog = {
            log = get_log_path("access_log", 80, "/place/db/www/logs");
            shared = {
              uuid = "7461816145880532667";
              report = {
                uuid = "service_total";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                cookie_policy = {
                  uuid = "service_total";
                  default_yandex_policies = "stable";
                  headers = {
                    create_func = {
                      ["X-Scheme"] = "scheme";
                    }; -- create_func
                    headers = {
                      copy_weak = {
                        ["X-Req-Id"] = "X-Request-Id";
                      }; -- copy_weak
                      headers = {
                        create_func_weak = {
                          ["X-Forwarded-For-Y"] = "realip";
                          ["X-Request-Id"] = "reqid";
                        }; -- create_func_weak
                        headers = {
                          copy = {
                            ["X-Forwarded-For-Y"] = "X-District-Forwarded-For";
                          }; -- copy
                          log_headers = {
                            name_re = "X-Request-Id|User-Agent|X-District-Action-Id|X-Initial-Request-Id|X-Client-Version|X-Source-File";
                            cookie_fields = "yandexuid";
                            regexp = {
                              ["awacs-balancer-health-check"] = {
                                priority = 12;
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
                                priority = 11;
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
                              ["l7-local-api"] = {
                                priority = 10;
                                match_and = {
                                  {
                                    match_fsm = {
                                      host = "yandex\\.(ru|by|uz|kz)";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                  };
                                  {
                                    match_fsm = {
                                      path = "/local/(s?)api/.*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                  };
                                }; -- match_and
                                report = {
                                  uuid = "local_l7";
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
                                        header_name = "Host";
                                        case_insensitive = false;
                                        regexp = "yandex\\.(.*)";
                                        rewrite = "local.yandex.%1";
                                      };
                                      {
                                        split = "url";
                                        global = false;
                                        rewrite = "%1";
                                        literal = false;
                                        regexp = "/local(.*)";
                                        case_insensitive = false;
                                      };
                                    }; -- actions
                                    shared = {
                                      uuid = "local-proxy";
                                    }; -- shared
                                  }; -- rewrite
                                }; -- report
                              }; -- ["l7-local-api"]
                              ["l7-local"] = {
                                priority = 9;
                                match_and = {
                                  {
                                    match_fsm = {
                                      host = "yandex\\.(ru|by|uz|kz|ua)";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                  };
                                  {
                                    match_fsm = {
                                      path = "/local.*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                  };
                                }; -- match_and
                                report = {
                                  uuid = "local";
                                  refers = "local";
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
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
                                        Location = "location";
                                      }; -- create_func
                                      rewrite = {
                                        actions = {
                                          {
                                            global = false;
                                            literal = false;
                                            case_insensitive = false;
                                            header_name = "Location";
                                            rewrite = "https://local.yandex.%1%2";
                                            regexp = "yandex\\.([a-z]+)/local(.*)";
                                          };
                                        }; -- actions
                                        errordocument = {
                                          status = 302;
                                          force_conn_close = false;
                                          remain_headers = "Location";
                                        }; -- errordocument
                                      }; -- rewrite
                                    }; -- headers
                                    refers = "local_l7";
                                  }; -- report
                                }; -- report
                              }; -- ["l7-local"]
                              http_to_https = {
                                priority = 8;
                                match_fsm = {
                                  header = {
                                    name = "X-Scheme";
                                    value = "http";
                                  }; -- header
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
                                          status = 302;
                                          force_conn_close = false;
                                          remain_headers = "Location";
                                        }; -- errordocument
                                      }; -- default
                                    }; -- regexp
                                  }; -- rewrite
                                }; -- headers
                              }; -- http_to_https
                              captcha = {
                                priority = 7;
                                match_fsm = {
                                  URI = "/x?(show|check)?captcha.*";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                report = {
                                  uuid = "captchasearch";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  h100 = {
                                    cutter = {
                                      bytes = 512;
                                      timeout = "0.1s";
                                      antirobot = {
                                        cut_request = true;
                                        no_cut_request_file = "./controls/no_cut_request_file";
                                        file_switch = "./controls/do.not.use.it";
                                        cut_request_bytes = 512;
                                        checker = {
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
                                                      { "man1-0313.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6033:92e2:baff:fe6e:bd84"; };
                                                      { "man1-0510.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:602f:92e2:baff:fe74:7dc4"; };
                                                      { "man1-0619.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6031:92e2:baff:fe74:7ada"; };
                                                      { "man1-0694.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7a2e"; };
                                                      { "man1-1193.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6012:f652:14ff:fe48:9680"; };
                                                      { "man1-1593.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6017:f652:14ff:fe8b:b020"; };
                                                      { "man1-1676.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:e4e0"; };
                                                      { "man1-1987.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:2320"; };
                                                      { "man1-2387.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6012:f652:14ff:fe8c:130"; };
                                                      { "man1-2710.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6078:f652:14ff:fe8b:f2f0"; };
                                                      { "man1-2858.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e6a0"; };
                                                      { "man1-2910.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:f170"; };
                                                      { "man1-3249.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6000:e61d:2dff:fe6d:bb30"; };
                                                      { "man1-3306.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6038:f652:14ff:fe8c:19e0"; };
                                                      { "man1-4045.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:603c:92e2:baff:fe6f:7fbc"; };
                                                      { "man2-4712.search.yandex.net"; 13512; 1062.000; "2a02:6b8:c01:761:0:604:dbc:a444"; };
                                                      { "man2-4804.search.yandex.net"; 13512; 1062.000; "2a02:6b8:c01:75e:0:604:dbc:a2f1"; };
                                                      { "man2-4997.search.yandex.net"; 13512; 1062.000; "2a02:6b8:c01:839:0:604:5cba:a2b2"; };
                                                      { "man2-5047.search.yandex.net"; 13512; 1062.000; "2a02:6b8:c01:766:0:604:dde:f430"; };
                                                      { "man2-5094.search.yandex.net"; 13512; 1062.000; "2a02:6b8:c01:839:0:604:5cba:a27a"; };
                                                      { "man2-6654.search.yandex.net"; 13512; 1062.000; "2a02:6b8:c01:645:0:604:14a9:6a7e"; };
                                                      { "man2-7104.search.yandex.net"; 13512; 1062.000; "2a02:6b8:c01:648:0:604:14a7:669f"; };
                                                      { "man2-7200.search.yandex.net"; 13512; 1062.000; "2a02:6b8:c01:651:0:604:14a9:69a5"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "30ms";
                                                      backend_timeout = "10s";
                                                      fail_on_5xx = true;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 0;
                                                      need_resolve = true;
                                                    }))
                                                  }; -- weighted2
                                                }; -- balancer2
                                              }; -- antirobot_man
                                              antirobot_sas = {
                                                weight = 1.000;
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
                                                      backend_timeout = "10s";
                                                      fail_on_5xx = true;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 0;
                                                      need_resolve = true;
                                                    }))
                                                  }; -- weighted2
                                                }; -- balancer2
                                              }; -- antirobot_sas
                                              antirobot_vla = {
                                                weight = 1.000;
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
                                                      { "vla1-1343.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:44:0:604:db7:a0b2"; };
                                                      { "vla1-1526.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:4d:0:604:db7:a142"; };
                                                      { "vla1-1797.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:45:0:604:db7:a64b"; };
                                                      { "vla1-2571.search.yandex.net"; 13512; 2042.000; "2a02:6b8:c0e:a2:0:604:db7:9b9d"; };
                                                      { "vla1-3421.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:11:0:604:db7:998f"; };
                                                      { "vla1-3568.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:a1:0:604:db7:a2db"; };
                                                      { "vla1-3679.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:87:0:604:db7:ab81"; };
                                                      { "vla1-3709.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:72:0:604:db7:a71b"; };
                                                      { "vla1-3710.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:80:0:604:db7:a836"; };
                                                      { "vla1-3716.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:72:0:604:db7:a5c6"; };
                                                      { "vla1-3863.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:99:0:604:db7:aa08"; };
                                                      { "vla1-3881.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:72:0:604:db7:a5e5"; };
                                                      { "vla1-3965.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:87:0:604:db7:aba1"; };
                                                      { "vla1-4006.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:80:0:604:db7:a92c"; };
                                                      { "vla1-4025.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:7f:0:604:db7:a3a5"; };
                                                      { "vla1-4041.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:8c:0:604:db7:abf2"; };
                                                      { "vla1-4063.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:8b:0:604:db7:aa8e"; };
                                                      { "vla1-4114.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:90:0:604:db7:aab9"; };
                                                      { "vla1-4117.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:8a:0:604:db7:a817"; };
                                                      { "vla1-4119.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:8a:0:604:db7:a978"; };
                                                      { "vla1-4130.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:99:0:604:db7:a8d6"; };
                                                      { "vla1-4153.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:9b:0:604:db7:aa91"; };
                                                      { "vla1-4167.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:9c:0:604:db7:a8e5"; };
                                                      { "vla1-4168.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:97:0:604:db7:a7a3"; };
                                                      { "vla1-4177.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:9b:0:604:db7:aa6b"; };
                                                      { "vla1-4183.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:7c:0:604:db7:9df2"; };
                                                      { "vla1-4192.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:8c:0:604:db7:ab53"; };
                                                      { "vla1-4200.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:90:0:604:db7:a82b"; };
                                                      { "vla1-4321.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:80:0:604:db7:a842"; };
                                                      { "vla1-4344.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:90:0:604:db7:ab5b"; };
                                                      { "vla1-4354.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:99:0:604:db7:aa94"; };
                                                      { "vla1-4385.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:63:0:604:db7:9e77"; };
                                                      { "vla1-4406.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:95:0:604:db7:a9f9"; };
                                                      { "vla1-4472.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:9c:0:604:db7:aa71"; };
                                                      { "vla1-4475.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:a1:0:604:db7:a2e0"; };
                                                      { "vla1-4553.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:77:0:604:d8f:eb26"; };
                                                      { "vla1-4554.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:77:0:604:d8f:eb76"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "30ms";
                                                      backend_timeout = "10s";
                                                      fail_on_5xx = true;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 0;
                                                      need_resolve = true;
                                                    }))
                                                  }; -- weighted2
                                                }; -- balancer2
                                              }; -- antirobot_vla
                                            }; -- rr
                                          }; -- balancer2
                                        }; -- checker
                                        module = {
                                          errordocument = {
                                            status = 403;
                                            force_conn_close = false;
                                          }; -- errordocument
                                        }; -- module
                                      }; -- antirobot
                                    }; -- cutter
                                  }; -- h100
                                }; -- report
                              }; -- captcha
                              ["local-images"] = {
                                priority = 6;
                                match_and = {
                                  {
                                    match_fsm = {
                                      host = "local\\.yandex\\.(ru|by|uz|kz)";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                  };
                                  {
                                    match_fsm = {
                                      path = "/api/attachments/image/.*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                  };
                                }; -- match_and
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
                                            uuid = "1220882788782260093";
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
                                                          { "man1-0313.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6033:92e2:baff:fe6e:bd84"; };
                                                          { "man1-0510.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:602f:92e2:baff:fe74:7dc4"; };
                                                          { "man1-0619.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6031:92e2:baff:fe74:7ada"; };
                                                          { "man1-0694.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7a2e"; };
                                                          { "man1-1193.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6012:f652:14ff:fe48:9680"; };
                                                          { "man1-1593.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6017:f652:14ff:fe8b:b020"; };
                                                          { "man1-1676.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:e4e0"; };
                                                          { "man1-1987.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:2320"; };
                                                          { "man1-2387.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6012:f652:14ff:fe8c:130"; };
                                                          { "man1-2710.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6078:f652:14ff:fe8b:f2f0"; };
                                                          { "man1-2858.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e6a0"; };
                                                          { "man1-2910.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:f170"; };
                                                          { "man1-3249.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6000:e61d:2dff:fe6d:bb30"; };
                                                          { "man1-3306.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:6038:f652:14ff:fe8c:19e0"; };
                                                          { "man1-4045.search.yandex.net"; 13512; 1062.000; "2a02:6b8:b000:603c:92e2:baff:fe6f:7fbc"; };
                                                          { "man2-4712.search.yandex.net"; 13512; 1062.000; "2a02:6b8:c01:761:0:604:dbc:a444"; };
                                                          { "man2-4804.search.yandex.net"; 13512; 1062.000; "2a02:6b8:c01:75e:0:604:dbc:a2f1"; };
                                                          { "man2-4997.search.yandex.net"; 13512; 1062.000; "2a02:6b8:c01:839:0:604:5cba:a2b2"; };
                                                          { "man2-5047.search.yandex.net"; 13512; 1062.000; "2a02:6b8:c01:766:0:604:dde:f430"; };
                                                          { "man2-5094.search.yandex.net"; 13512; 1062.000; "2a02:6b8:c01:839:0:604:5cba:a27a"; };
                                                          { "man2-6654.search.yandex.net"; 13512; 1062.000; "2a02:6b8:c01:645:0:604:14a9:6a7e"; };
                                                          { "man2-7104.search.yandex.net"; 13512; 1062.000; "2a02:6b8:c01:648:0:604:14a7:669f"; };
                                                          { "man2-7200.search.yandex.net"; 13512; 1062.000; "2a02:6b8:c01:651:0:604:14a9:69a5"; };
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
                                                          { "vla1-1343.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:44:0:604:db7:a0b2"; };
                                                          { "vla1-1526.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:4d:0:604:db7:a142"; };
                                                          { "vla1-1797.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:45:0:604:db7:a64b"; };
                                                          { "vla1-2571.search.yandex.net"; 13512; 2042.000; "2a02:6b8:c0e:a2:0:604:db7:9b9d"; };
                                                          { "vla1-3421.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:11:0:604:db7:998f"; };
                                                          { "vla1-3568.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:a1:0:604:db7:a2db"; };
                                                          { "vla1-3679.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:87:0:604:db7:ab81"; };
                                                          { "vla1-3709.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:72:0:604:db7:a71b"; };
                                                          { "vla1-3710.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:80:0:604:db7:a836"; };
                                                          { "vla1-3716.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:72:0:604:db7:a5c6"; };
                                                          { "vla1-3863.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:99:0:604:db7:aa08"; };
                                                          { "vla1-3881.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:72:0:604:db7:a5e5"; };
                                                          { "vla1-3965.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:87:0:604:db7:aba1"; };
                                                          { "vla1-4006.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:80:0:604:db7:a92c"; };
                                                          { "vla1-4025.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:7f:0:604:db7:a3a5"; };
                                                          { "vla1-4041.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:8c:0:604:db7:abf2"; };
                                                          { "vla1-4063.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:8b:0:604:db7:aa8e"; };
                                                          { "vla1-4114.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:90:0:604:db7:aab9"; };
                                                          { "vla1-4117.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:8a:0:604:db7:a817"; };
                                                          { "vla1-4119.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:8a:0:604:db7:a978"; };
                                                          { "vla1-4130.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:99:0:604:db7:a8d6"; };
                                                          { "vla1-4153.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:9b:0:604:db7:aa91"; };
                                                          { "vla1-4167.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:9c:0:604:db7:a8e5"; };
                                                          { "vla1-4168.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:97:0:604:db7:a7a3"; };
                                                          { "vla1-4177.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:9b:0:604:db7:aa6b"; };
                                                          { "vla1-4183.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:7c:0:604:db7:9df2"; };
                                                          { "vla1-4192.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:8c:0:604:db7:ab53"; };
                                                          { "vla1-4200.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:90:0:604:db7:a82b"; };
                                                          { "vla1-4321.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:80:0:604:db7:a842"; };
                                                          { "vla1-4344.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:90:0:604:db7:ab5b"; };
                                                          { "vla1-4354.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:99:0:604:db7:aa94"; };
                                                          { "vla1-4385.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:63:0:604:db7:9e77"; };
                                                          { "vla1-4406.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:95:0:604:db7:a9f9"; };
                                                          { "vla1-4472.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:9c:0:604:db7:aa71"; };
                                                          { "vla1-4475.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:a1:0:604:db7:a2e0"; };
                                                          { "vla1-4553.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:77:0:604:d8f:eb26"; };
                                                          { "vla1-4554.search.yandex.net"; 13512; 1866.000; "2a02:6b8:c0e:77:0:604:d8f:eb76"; };
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
                                          }; -- shared
                                        }; -- checker
                                        module = {
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
                                                service_name = "local";
                                                service_name_header = "Y-Service";
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
                                                report = {
                                                  refers = "local,local";
                                                  just_storage = false;
                                                  disable_robotness = true;
                                                  disable_sslness = true;
                                                  events = {
                                                    stats = "report";
                                                  }; -- events
                                                  report = {
                                                    uuid = "local_images";
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
                                                      connection_attempts = 5;
                                                      rr = {
                                                        unpack(gen_proxy_backends({
                                                          { "district-int.stable.qloud-b.yandex.net"; 80; 1.000; "2a02:6b8:0:3400::3a7"; };
                                                        }, {
                                                          resolve_timeout = "10ms";
                                                          connect_timeout = "100ms";
                                                          backend_timeout = "300s";
                                                          fail_on_5xx = false;
                                                          http_backend = true;
                                                          buffering = false;
                                                          keepalive_count = 1;
                                                          need_resolve = true;
                                                          client_read_timeout = "30s";
                                                          client_write_timeout = "30s";
                                                        }))
                                                      }; -- rr
                                                      on_error = {
                                                        errordocument = {
                                                          status = 502;
                                                          force_conn_close = false;
                                                          content = "Local unavailable";
                                                        }; -- errordocument
                                                      }; -- on_error
                                                    }; -- balancer2
                                                  }; -- report
                                                }; -- report
                                              }; -- exp_getter
                                            }; -- headers
                                          }; -- geobase
                                        }; -- module
                                      }; -- antirobot
                                    }; -- cutter
                                  }; -- h100
                                }; -- hasher
                              }; -- ["local-images"]
                              ["local-sitemap"] = {
                                priority = 5;
                                match_and = {
                                  {
                                    match_fsm = {
                                      host = "local\\.yandex\\.(ru|by|uz|kz)";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                  };
                                  {
                                    match_or = {
                                      {
                                        match_fsm = {
                                          path = "/sitemap/.+";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                      };
                                      {
                                        match_fsm = {
                                          path = "/sitemap\\.xml";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                      };
                                    }; -- match_or
                                  };
                                }; -- match_and
                                report = {
                                  refers = "local,local";
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  report = {
                                    uuid = "local_sitemap";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    headers = {
                                      create = {
                                        Host = "s3.mds.yandex.net";
                                      }; -- create
                                      rewrite = {
                                        actions = {
                                          {
                                            global = false;
                                            literal = false;
                                            case_insensitive = false;
                                            regexp = "^/sitemap\\.xml";
                                            rewrite = "/sitemap/sitemap.xml";
                                          };
                                          {
                                            global = false;
                                            literal = false;
                                            case_insensitive = false;
                                            regexp = "^/sitemap/(.+)";
                                            rewrite = "/district-sitemap-v2/%1";
                                          };
                                        }; -- actions
                                        balancer2 = {
                                          simple_policy = {};
                                          attempts = 2;
                                          connection_attempts = 5;
                                          return_last_5xx = true;
                                          status_code_blacklist = {
                                            "5xx";
                                          }; -- status_code_blacklist
                                          rr = {
                                            unpack(gen_proxy_backends({
                                              { "s3.mds.yandex.net"; 80; 1.000; "2a02:6b8:0:3400::3:147"; };
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
                                          }; -- rr
                                          on_error = {
                                            errordocument = {
                                              status = 502;
                                              force_conn_close = false;
                                              content = "Local sitemap unavailable";
                                            }; -- errordocument
                                          }; -- on_error
                                        }; -- balancer2
                                      }; -- rewrite
                                    }; -- headers
                                  }; -- report
                                }; -- report
                              }; -- ["local-sitemap"]
                              ["local-static"] = {
                                priority = 4;
                                match_and = {
                                  {
                                    match_fsm = {
                                      host = "local\\.yandex\\.(ru|by|uz|kz)";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                  };
                                  {
                                    match_or = {
                                      {
                                        match_fsm = {
                                          path = "/\\.well-known/.+";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                      };
                                    }; -- match_or
                                  };
                                }; -- match_and
                                report = {
                                  refers = "local,local";
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  report = {
                                    uuid = "local_static";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    headers = {
                                      create = {
                                        Host = "s3.mds.yandex.net";
                                      }; -- create
                                      rewrite = {
                                        actions = {
                                          {
                                            global = false;
                                            literal = false;
                                            regexp = "^/(.+)";
                                            case_insensitive = false;
                                            rewrite = "/district-static/%1";
                                          };
                                        }; -- actions
                                        balancer2 = {
                                          simple_policy = {};
                                          attempts = 2;
                                          connection_attempts = 5;
                                          return_last_5xx = true;
                                          status_code_blacklist = {
                                            "5xx";
                                          }; -- status_code_blacklist
                                          rr = {
                                            unpack(gen_proxy_backends({
                                              { "s3.mds.yandex.net"; 80; 1.000; "2a02:6b8:0:3400::3:147"; };
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
                                          }; -- rr
                                          on_error = {
                                            errordocument = {
                                              status = 502;
                                              force_conn_close = false;
                                              content = "Local static unavailable";
                                            }; -- errordocument
                                          }; -- on_error
                                        }; -- balancer2
                                      }; -- rewrite
                                    }; -- headers
                                  }; -- report
                                }; -- report
                              }; -- ["local-static"]
                              ["local-stream-export"] = {
                                priority = 3;
                                match_and = {
                                  {
                                    match_fsm = {
                                      host = "local\\.yandex\\.(ru|by|uz|kz)";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                  };
                                  {
                                    match_fsm = {
                                      path = "/stream/.+";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                  };
                                }; -- match_and
                                report = {
                                  refers = "local,local";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  report = {
                                    uuid = "local_stream";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    headers = {
                                      create = {
                                        Host = "s3.mds.yandex.net";
                                      }; -- create
                                      rewrite = {
                                        actions = {
                                          {
                                            global = false;
                                            literal = false;
                                            case_insensitive = false;
                                            regexp = "^/stream/(.+)";
                                            rewrite = "/district-stream/%1";
                                          };
                                        }; -- actions
                                        balancer2 = {
                                          simple_policy = {};
                                          attempts = 2;
                                          connection_attempts = 5;
                                          return_last_5xx = true;
                                          status_code_blacklist = {
                                            "5xx";
                                          }; -- status_code_blacklist
                                          rr = {
                                            unpack(gen_proxy_backends({
                                              { "s3.mds.yandex.net"; 80; 1.000; "2a02:6b8:0:3400::3:147"; };
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
                                          }; -- rr
                                          on_error = {
                                            errordocument = {
                                              status = 502;
                                              force_conn_close = false;
                                              content = "File unavailable";
                                            }; -- errordocument
                                          }; -- on_error
                                        }; -- balancer2
                                      }; -- rewrite
                                    }; -- headers
                                  }; -- report
                                }; -- report
                              }; -- ["local-stream-export"]
                              ["local"] = {
                                priority = 2;
                                match_fsm = {
                                  host = "local\\.yandex\\.(ru|by|uz|kz)";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
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
                                            uuid = "1220882788782260093";
                                          }; -- shared
                                        }; -- checker
                                        module = {
                                          threshold = {
                                            lo_bytes = 1024;
                                            hi_bytes = 1024;
                                            recv_timeout = "5s";
                                            pass_timeout = "15s";
                                            on_pass_timeout_failure = {
                                              errordocument = {
                                                status = 408;
                                                force_conn_close = false;
                                                content = "Request Timeout";
                                              }; -- errordocument
                                            }; -- on_pass_timeout_failure
                                            cookie_hasher = {
                                              cookie = "yandexuid";
                                              file_switch = "./controls/disable_cookie_hasher";
                                              balancer2 = {
                                                unique_policy = {};
                                                attempts = 1;
                                                rendezvous_hashing = {
                                                  weights_file = "./controls/traffic_control.weights";
                                                  local_qloud = {
                                                    weight = 100.000;
                                                    shared = {
                                                      uuid = "local-proxy";
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
                                                            service_name = "local";
                                                            service_name_header = "Y-Service";
                                                            uaas = {
                                                              shared = {
                                                                uuid = "8988526946134060236";
                                                              }; -- shared
                                                            }; -- uaas
                                                            report = {
                                                              refers = "local,local";
                                                              ranges = get_str_var("default_ranges");
                                                              matcher_map = {
                                                                www = {
                                                                  match_not = {
                                                                    match_fsm = {
                                                                      URI = "/api/.*";
                                                                      case_insensitive = true;
                                                                      surround = false;
                                                                    }; -- match_fsm
                                                                  }; -- match_not
                                                                }; -- www
                                                                api = {
                                                                  match_fsm = {
                                                                    URI = "/api/.*";
                                                                    case_insensitive = true;
                                                                    surround = false;
                                                                  }; -- match_fsm
                                                                }; -- api
                                                              }; -- matcher_map
                                                              just_storage = false;
                                                              disable_robotness = true;
                                                              disable_sslness = true;
                                                              events = {
                                                                stats = "report";
                                                              }; -- events
                                                              report = {
                                                                uuid = "local_common";
                                                                ranges = get_str_var("default_ranges");
                                                                just_storage = false;
                                                                disable_robotness = true;
                                                                disable_sslness = true;
                                                                events = {
                                                                  stats = "report";
                                                                }; -- events
                                                                balancer2 = {
                                                                  timeout_policy = {
                                                                    timeout = "15s";
                                                                    retry_policy = {
                                                                      unique_policy = {};
                                                                    }; -- retry_policy
                                                                  }; -- timeout_policy
                                                                  attempts = 2;
                                                                  connection_attempts = 5;
                                                                  return_last_5xx = true;
                                                                  status_code_blacklist = {
                                                                    "5xx";
                                                                  }; -- status_code_blacklist
                                                                  rr = {
                                                                    unpack(gen_proxy_backends({
                                                                      { "district-int.stable.qloud-b.yandex.net"; 80; 1.000; "2a02:6b8:0:3400::3a7"; };
                                                                    }, {
                                                                      resolve_timeout = "10ms";
                                                                      connect_timeout = "100ms";
                                                                      backend_timeout = "10s";
                                                                      fail_on_5xx = false;
                                                                      http_backend = true;
                                                                      buffering = false;
                                                                      keepalive_count = 3;
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
                                                            }; -- report
                                                          }; -- exp_getter
                                                        }; -- headers
                                                      }; -- geobase
                                                    }; -- shared
                                                  }; -- local_qloud
                                                  local_pumpkin = {
                                                    weight = -1.000;
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
                                                      report = {
                                                        refers = "local,local";
                                                        just_storage = false;
                                                        disable_robotness = true;
                                                        disable_sslness = true;
                                                        events = {
                                                          stats = "report";
                                                        }; -- events
                                                        report = {
                                                          uuid = "local_pumpkin";
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
                                                            connection_attempts = 5;
                                                            sd = {
                                                              endpoint_sets = {
                                                                {
                                                                  cluster_name = "sas";
                                                                  endpoint_set_id = "district-pumpkin-prod.pumpkin";
                                                                };
                                                                {
                                                                  cluster_name = "man";
                                                                  endpoint_set_id = "district-pumpkin-prod.pumpkin";
                                                                };
                                                                {
                                                                  cluster_name = "vla";
                                                                  endpoint_set_id = "district-pumpkin-prod.pumpkin";
                                                                };
                                                              }; -- endpoint_sets
                                                              proxy_options = {
                                                                resolve_timeout = "10ms";
                                                                connect_timeout = "80ms";
                                                                backend_timeout = "3s";
                                                                fail_on_5xx = true;
                                                                http_backend = true;
                                                                buffering = false;
                                                                keepalive_count = 1;
                                                                need_resolve = false;
                                                              }; -- proxy_options
                                                              dynamic = {
                                                                max_pessimized_share = 0.300;
                                                                min_pessimization_coeff = 0.100;
                                                                weight_increase_step = 0.100;
                                                                history_interval = "10s";
                                                                backends_name = "pumpkin";
                                                              }; -- dynamic
                                                            }; -- sd
                                                            attempts_rate_limiter = {
                                                              limit = 0.500;
                                                              coeff = 0.990;
                                                              switch_default = true;
                                                            }; -- attempts_rate_limiter
                                                          }; -- balancer2
                                                        }; -- report
                                                      }; -- report
                                                    }; -- geobase
                                                  }; -- local_pumpkin
                                                }; -- rendezvous_hashing
                                                on_error = {
                                                  errordocument = {
                                                    status = 502;
                                                    force_conn_close = false;
                                                    content = "Local unavailable";
                                                  }; -- errordocument
                                                }; -- on_error
                                              }; -- balancer2
                                            }; -- cookie_hasher
                                          }; -- threshold
                                        }; -- module
                                      }; -- antirobot
                                    }; -- cutter
                                  }; -- h100
                                }; -- hasher
                              }; -- ["local"]
                              default = {
                                priority = 1;
                                errordocument = {
                                  status = 404;
                                  force_conn_close = false;
                                  content = "No such upstream";
                                }; -- errordocument
                              }; -- default
                            }; -- regexp
                          }; -- log_headers
                        }; -- headers
                      }; -- headers
                    }; -- headers
                  }; -- headers
                }; -- cookie_policy
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
              priv = get_private_cert_path("local.yandex.ru.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-local.yandex.ru.pem", "/dev/shm/balancer");
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.local.yandex.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.local.yandex.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.local.yandex.ru.key", "/dev/shm/balancer/priv");
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
                uuid = "7461816145880532667";
              }; -- shared
            }; -- accesslog
          }; -- http
        }; -- ssl_sni
      }; -- errorlog
    }; -- https_section
  }; -- ipdispatch
}